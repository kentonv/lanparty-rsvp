package com.google.kentonshouse.cloud;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.gson.JsonSyntaxException;

@SuppressWarnings("serial")
public class ApproveServlet extends HttpServlet {
  static class Request {
    String userId;
  }
  
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Request request;
    try {
      request = Singletons.GSON.fromJson(req.getReader(), Request.class);
    } catch (JsonSyntaxException e) {
      resp.sendError(400, "Couldn't parse JSON request: " + e.getMessage());
      return;
    }
    
    PreparedQuery pq = Singletons.DATASTORE.prepare(new Query("user").setFilter(
        new Query.FilterPredicate("id", FilterOperator.EQUAL, request.userId)));
    Entity userEntity = pq.asSingleEntity();
    if (userEntity == null) {
      userEntity = new Entity("user");
      userEntity.setProperty("id", request.userId);
    }
    userEntity.setProperty("approved", true);
    Singletons.DATASTORE.put(userEntity);
    
    resp.setContentType("application/json");
    Singletons.GSON.toJson(new Object(), resp.getWriter());
  }
}
