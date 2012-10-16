package com.google.kentonshouse.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.users.User;
import com.google.gson.JsonSyntaxException;
import com.google.kentonshouse.cloud.EventInfo.Context;

@SuppressWarnings("serial")
public class RegisterServlet extends HttpServlet {
  static class UserInfo {
    UserInfo() {}
    UserInfo(User user, String url) {
      this(user, url, getUserEntity(user));
    }
    UserInfo(User user, String url, Entity entity) {
      email = user.getEmail();
      logoutUrl = Singletons.USER_SERVICE.createLogoutURL(url);
      
      approved = entity != null && ((Boolean) entity.getProperty("approved"));
      name = entity == null ? null : (String) entity.getProperty("name");
      
      if (approved) {
        eventLocation = Singletons.getEventLocation();
      }
    }
    
    private static Entity getUserEntity(User user) {
      PreparedQuery pq = Singletons.DATASTORE.prepare(new Query("user").setFilter(
          new Query.FilterPredicate("id", FilterOperator.EQUAL, user.getUserId())));
      Entity entity = pq.asSingleEntity();
      return entity;
    }
    
    String name;
    String email;
    String logoutUrl;
    boolean approved = false;
    Singletons.EventLocation eventLocation;
  }
  
  static class Request {
    String eventKey;
    EventInfo.Attendee attendee;
  }
  
  static class Response {
    UserInfo userInfo;
    List<EventInfo> events;
  }
  
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    StringBuffer url = req.getRequestURL();
    url.replace(url.lastIndexOf("/api/"), url.length(), "/");
    UserInfo userInfo = new UserInfo(Singletons.USER_SERVICE.getCurrentUser(), url.toString());
    
    EventInfo.Context context = getCentxt(userInfo);
    
    Response response = makeResponse(userInfo, context);
    
    resp.setContentType("application/json");
    Singletons.GSON.toJson(response, resp.getWriter());
  }

  private Response makeResponse(UserInfo userInfo, EventInfo.Context context) {
    // Get future events.
    PreparedQuery pq = Singletons.DATASTORE.prepare(new Query("event")
        .setFilter(new Query.FilterPredicate("end", FilterOperator.GREATER_THAN, new Date()))
        .addSort("end", SortDirection.ASCENDING));
    List<Entity> eventEntities = pq.asList(FetchOptions.Builder.withDefaults());
    List<EventInfo> events = new ArrayList<EventInfo>();
    for (Entity entity : eventEntities) {
      events.add(new EventInfo(entity, context));
    }
    
    Response response = new Response();
    response.events = events;
    response.userInfo = userInfo;
    return response;
  }

  private EventInfo.Context getCentxt(UserInfo userInfo) {
    EventInfo.Context context;
    if (Singletons.USER_SERVICE.isUserAdmin()) {
      context = Context.ADMIN;
    } else if (userInfo.approved) {
      context = Context.APPROVED;
    } else {
      context = Context.UNAPPROVED;
    }
    return context;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Request request;
    try {
      request = Singletons.GSON.fromJson(req.getReader(), Request.class);
    } catch (JsonSyntaxException e) {
      resp.sendError(400, "Couldn't parse JSON request: " + e.getMessage());
      return;
    }
    
    if (request.eventKey == null) {
      resp.sendError(400, "Missing eventKey.");
      return;
    }
    
    User user = Singletons.USER_SERVICE.getCurrentUser();
    String userId = user.getUserId();
    
    if (request.attendee != null) {
      String error = request.attendee.validate();
      if (error != null) {
        resp.sendError(400, error);
        return;
      }
      
      request.attendee.userId = userId;
      request.attendee.email = user.getEmail();
    }
    
    PreparedQuery pq = Singletons.DATASTORE.prepare(new Query("user").setFilter(
        new Query.FilterPredicate("id", FilterOperator.EQUAL, userId)));
    Entity userEntity = pq.asSingleEntity();
    if (userEntity == null) {
      userEntity = new Entity("user");
      userEntity.setProperty("id", userId);
      userEntity.setProperty("approved", false);
    }
    if (request.attendee != null) {
      userEntity.setProperty("name", request.attendee.name);
    }
    Singletons.DATASTORE.put(userEntity);
    
    Transaction txn = Singletons.DATASTORE.beginTransaction();
    
    Entity eventEntity;
    try {
      // Get event from datastore.
      try {
        eventEntity = Singletons.DATASTORE.get(txn, KeyFactory.stringToKey(request.eventKey));
      } catch (EntityNotFoundException e) {
        resp.sendError(400, "No such event.");
        return;
      }
      EventInfo event = new EventInfo(eventEntity, EventInfo.Context.RAW);
      
      // Add the new attendee, replacing any existing one with the same user ID.
      List<EventInfo.Attendee> newAttendees = new ArrayList<EventInfo.Attendee>();
      if (event.attendees != null) {
        newAttendees.addAll(event.attendees);
      }
      boolean found = false;
      for (int i = 0; i < newAttendees.size(); i++) {
        if (newAttendees.get(i).userId.equals(userId)) {
          if (request.attendee == null) {
            newAttendees.remove(i);
          } else {
            newAttendees.set(i, request.attendee);
          }
          found = true;
          break;
        }
      }
      if (!found && request.attendee != null) {
        newAttendees.add(request.attendee);
      }
      
      // Write back to datastore.
      eventEntity.setProperty("attendees", new Text(Singletons.GSON.toJson(newAttendees)));
      Singletons.DATASTORE.put(txn, eventEntity);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
    
    // Build a new world state.
    
    UserInfo userInfo = new UserInfo(user, req.getRequestURL().toString(), userEntity);
    EventInfo.Context context = getCentxt(userInfo);
    Response response = makeResponse(userInfo, context);
    
    // Substitute in our new event.
    EventInfo newEvent = new EventInfo(eventEntity, context);
    for (int i = 0; i < response.events.size(); i++) {
      if (response.events.get(i).key.equals(newEvent.key)) {
        response.events.set(i, newEvent);
        break;
      }
    }
    
    resp.setContentType("application/json");
    Singletons.GSON.toJson(response, resp.getWriter());
  }
}
