package com.google.kentonshouse.cloud;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.users.User;
import com.google.gson.reflect.TypeToken;
import com.google.kentonshouse.cloud.EventInfo.Attendee.Privacy;

class EventInfo {
  String key = null;
  long startTime = 0;
  long endTime = 0;
  String theme;
  String themeDescription;
  ArrayList<Attendee> attendees;
  int anonymousAttendees = 0;
  
  static class Attendee {
    enum Probability { definitely, probably, maybe }
    enum Duration { attending, droppingBy }
    enum Activity { playing, watching }
    enum Privacy { everyone, approved, adminOnly }
    enum Contribution { snacks, beverages, alcohol, cash }
    
    Probability probability;
    Duration duration;
    Activity activity;
    
    String name;
    Privacy namePrivacy;
    HashSet<Contribution> contributions;
    boolean haveLaptop;
    String notes;
    
    String userId;
    String email;
    Boolean isMe;
    Boolean approved;
    
    private void removeAdminOnlyInfo(String callerId) {
      if (isUser(callerId)) {
        isMe = true;
        userId = null;
      } else {
        namePrivacy = null;
        userId = null;
      }
      approved = null;
      email = null;
    }

    private boolean isUser(String callerId) {
      return userId != null && userId.equals(callerId);
    }

    public String validate() {
      if (probability == null || duration == null || activity == null ||
          name == null || namePrivacy == null || contributions == null ||
          notes == null) {
        return "Missing fields.";
      }
      if (isMe != null || approved != null || userId != null || email != null) {
        return "I don't think so.";
      }
      return null;
    }
  }
  
  EventInfo() {}
  EventInfo(Entity entity, Context context) {
    key = KeyFactory.keyToString(entity.getKey());
    startTime = ((Date) entity.getProperty("start")).getTime();
    endTime = ((Date) entity.getProperty("end")).getTime();
    theme = (String) entity.getProperty("theme");
    Object tdProp = entity.getProperty("themeDescription");
    themeDescription = (tdProp instanceof Text) ? ((Text) tdProp).getValue() : ((String) tdProp);
    
    Text attendeeText = (Text) entity.getProperty("attendees");
    if (attendeeText != null) {
      attendees = Singletons.GSON.fromJson(attendeeText.getValue(), ATTENDEE_LIST_TYPE);
    }
    
    filterFor(context);
  }
  
  static Type ATTENDEE_LIST_TYPE = new TypeToken<List<Attendee>>(){}.getType();
  
  String validate() {
    if (startTime == 0) {
      return "Missing start time.";
    }
    if (endTime == 0) {
      return "Missing end time.";
    }
    return null;
  }
  
  Entity toEntity() {
    Entity entity = new Entity("event");
    updateEntityMetadata(entity);
    
    if (attendees != null) {
      entity.setProperty("attendees", new Text(Singletons.GSON.toJson(attendees)));
    }
    return entity;
  }
  
  void updateEntityMetadata(Entity entity) {
    entity.setProperty("start", new Date(startTime));
    entity.setProperty("end", new Date(endTime));
    if (theme != null && !theme.isEmpty()) {
      entity.setProperty("theme", theme);
    } else {
      entity.removeProperty("theme");
    }
    if (themeDescription != null && !themeDescription.isEmpty()) {
      entity.setProperty("themeDescription", new Text(themeDescription));
    } else {
      entity.removeProperty("themeDescription");
    }
  }
  
  enum Context {
    FRONT_PAGE,
    UNAPPROVED,
    APPROVED,
    ADMIN,
    RAW
  }
  
  private void filterFor(Context context) {
    User caller = (context == Context.FRONT_PAGE || context == Context.RAW) ? 
        null : Singletons.USER_SERVICE.getCurrentUser();
    String callerId = caller != null ? caller.getUserId() : null;
    
    switch (context) {
      case FRONT_PAGE:
        attendees = null;
        break;
      case UNAPPROVED: {
        ArrayList<Attendee> filteredList = new ArrayList<Attendee>();
        Set<String> approvedUsers = Singletons.getApprovedUsers();
        for (Attendee attendee: attendees) {
          if (attendee.isUser(callerId) ||
              (attendee.namePrivacy == Privacy.everyone && 
               approvedUsers.contains(attendee.userId))) {
            attendee.removeAdminOnlyInfo(callerId);
            filteredList.add(attendee);
          } else if (approvedUsers.contains(attendee.userId)) {
            ++anonymousAttendees;
          }
        }
        attendees = filteredList;
        break;
      }
      case APPROVED: {
        ArrayList<Attendee> filteredList = new ArrayList<Attendee>();
        Set<String> approvedUsers = Singletons.getApprovedUsers();
        for (Attendee attendee: attendees) {
          if (attendee.isUser(callerId) || 
              (attendee.namePrivacy != Privacy.adminOnly &&
               approvedUsers.contains(attendee.userId))) {
            attendee.removeAdminOnlyInfo(callerId);
            filteredList.add(attendee);
          } else if (approvedUsers.contains(attendee.userId)) {
            ++anonymousAttendees;
          }
        }
        attendees = filteredList;
        break;
      }
      case ADMIN: {
        Set<String> approvedUsers = Singletons.getApprovedUsers();
        for (Attendee attendee: attendees) {
          attendee.isMe = attendee.isUser(callerId) ? true : null;
          attendee.approved = approvedUsers.contains(attendee.userId);
        }
        break;
      }
      case RAW:
        break;
    }
  }
}