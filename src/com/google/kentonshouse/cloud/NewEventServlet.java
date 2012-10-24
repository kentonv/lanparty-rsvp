package com.google.kentonshouse.cloud;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.gson.JsonSyntaxException;

@SuppressWarnings("serial")
public class NewEventServlet extends HttpServlet {
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    EventInfo eventInfo;
    try {
      eventInfo = Singletons.GSON.fromJson(req.getReader(), EventInfo.class);
    } catch (JsonSyntaxException e) {
      resp.sendError(400, "Couldn't parse JSON request: " + e.getMessage());
      return;
    }
    eventInfo.attendees = new ArrayList<EventInfo.Attendee>();
    
    String error = eventInfo.validate();
    if (error != null) {
      resp.sendError(400, error);
      return;
    }
    
    Key key;
    if (eventInfo.key == null) {
      key = Singletons.DATASTORE.put(eventInfo.toEntity());
    } else {
      key = KeyFactory.stringToKey(eventInfo.key);
      try {
        Entity existing = Singletons.DATASTORE.get(key);
        eventInfo.updateEntityMetadata(existing);
        key = Singletons.DATASTORE.put(existing);
      } catch (EntityNotFoundException e) {
        resp.sendError(404, "No such existing event: " + eventInfo.key);
        return;
      }
    }
    
    resp.setContentType("application/json");
    resp.getWriter().println("{ \"key\": \"" + KeyFactory.keyToString(key) + "\" }");
  }
}
