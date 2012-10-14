package com.google.kentonshouse.cloud;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;

@SuppressWarnings("serial")
public class NextEventDateServlet extends HttpServlet {
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    PreparedQuery pq = Singletons.DATASTORE.prepare(new Query("event")
        .setFilter(new Query.FilterPredicate("end", FilterOperator.GREATER_THAN, new Date()))
        .addSort("end", SortDirection.ASCENDING));
    
    List<Entity> results = pq.asList(FetchOptions.Builder.withLimit(1));
    
    List<EventInfo> events = new ArrayList<EventInfo>();
    
    for (Entity result : results) {
      events.add(new EventInfo(result, EventInfo.Context.FRONT_PAGE));
    }
    
    resp.setContentType("application/json");
    Singletons.GSON.toJson(events, resp.getWriter());
  }
}
