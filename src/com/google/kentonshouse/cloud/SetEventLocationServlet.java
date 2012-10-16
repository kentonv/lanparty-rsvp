package com.google.kentonshouse.cloud;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonSyntaxException;

@SuppressWarnings("serial")
public class SetEventLocationServlet extends HttpServlet {
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    Singletons.EventLocation location;
    try {
      location = Singletons.GSON.fromJson(req.getReader(), Singletons.EventLocation.class);
    } catch (JsonSyntaxException e) {
      resp.sendError(400, "Couldn't parse JSON request: " + e.getMessage());
      return;
    }
    
    Singletons.setEventLocation(location);
    
    resp.setContentType("application/json");
    Singletons.GSON.toJson(new Object(), resp.getWriter());
  }
}
