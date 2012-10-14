package com.google.kentonshouse.cloud;

import java.util.HashSet;
import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.FetchOptions.Builder;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class Singletons {
  static Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  static final UserService USER_SERVICE = UserServiceFactory.getUserService();
  static final DatastoreService DATASTORE = DatastoreServiceFactory.getDatastoreService();
  static Set<String> getApprovedUsers() {
    Set<String> result = new HashSet<String>();
    PreparedQuery pq = DATASTORE.prepare(new Query("user").setFilter(
        new Query.FilterPredicate("approved", FilterOperator.EQUAL, true)));
    for (Entity user: pq.asIterable(FetchOptions.Builder.withDefaults())) {
      result.add((String) user.getProperty("id"));
    }
    return result;
  }
}
