// This code totally sucks but I don't give a shit.

var isMobile = navigator.userAgent.match(/Android/i) ||
    navigator.userAgent.match(/BlackBerry/i) ||
    navigator.userAgent.match(/iPhone|iPad|iPod/i) ||
    navigator.userAgent.match(/Opera Mini/i) ||
    navigator.userAgent.match(/IEMobile/i);

function get(url, callback) {
  var request = new XMLHttpRequest();
  request.onreadystatechange = function() {
    if (request.readyState == XMLHttpRequest.DONE) {
      if (request.status == 200) {
        callback(JSON.parse(request.responseText));
      } else {
        alert("Request to server failed: " + request.status + "\n" + request.responseText);
      }
    }
  };
  request.open("GET", url, true);
  request.send(null);
}

function post(url, data, callback) {
  var request = new XMLHttpRequest();
  request.onreadystatechange = function() {
    if (request.readyState == XMLHttpRequest.DONE) {
      if (request.status == 200) {
        callback(JSON.parse(request.responseText));
      } else {
        alert("Request to server failed: " + request.status + "\n" + request.responseText);
      }
    }
  };
  request.open("POST", url, true);
  request.send(JSON.stringify(data));
}

function nextSaturday() {
  var now = new Date();
  var day = now.getDay();
  var saturday = new Date(now.getTime() + (6 - day) * 86400000);
  saturday.setHours(0, 0, 0, 0);
  return saturday;
}

function hourToString(hour) {
  if (hour < 12) {
    return hour + "AM";
  } else if (hour == 12) {
    return "noon";
  } else if (hour < 24) {
    return (hour - 12) + "PM";
  } else if (hour == 24) {
    return "midnight";
  } else {
    return hourToString(hour - 24) + " (next day)";
  }
}

function removeChildren(element) {
  while (element.childNodes.length > 0) {
    element.removeChild(element.childNodes[0]);
  }
}

function setText(element, text) {
  removeChildren(element);
  element.appendChild(document.createTextNode(text));
}

function setTextWithLineBreaks(element, text) {
  removeChildren(element);
  
  var lines = text.split("\n");
  
  for (var i in lines) {
    if (i > 0) {
      element.appendChild(document.createElement("br"));
    }
    element.appendChild(document.createTextNode(lines[i]));
  }
}

var MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

//=======================================================================================

function setFrontPageInfo(yesno, info) {
  setText(document.getElementById("yesno"), yesno);
  setText(document.getElementById("next-info"), info);
}

function startupFrontPage() {
  document.body.className = isMobile ? "mobile" : "desktop";
  
  get("/api/next-event", function(response) {
    if (response.loggedIn) {
      document.getElementById("register-button").onclick = register;
    }
    
    var events = response.events;
    if (events.length > 0) {
      var date = new Date(events[0].startTime);
      var endHour = (events[0].endTime - events[0].startTime) / 3600000 + date.getHours();
      
      var endOfNextSaturday = nextSaturday().getTime() + 86400000;
      
      setFrontPageInfo(events[0].startTime < endOfNextSaturday ? "YES" : "NO",
          "The next LAN party is: " + MONTHS[date.getMonth()] + " " + date.getDate() +
          " " + hourToString(date.getHours()) + " to " + hourToString(endHour));
      document.getElementById("register").style.display = "block";
    } else {
      setFrontPageInfo("UNKNOWN", "No future LAN party has been scheduled!");
    }
  });
}

function showLoginWarning() {
  document.getElementById("yesno").style.display = "none";
  document.getElementById("next-info").style.display = "none";
  document.getElementById("login-warning").style.display = "block";
  
  var button = document.getElementById("register-button");
  setText(button, "OK, got it >>");
  button.onclick = register;
}

function register() {
  window.location.pathname = "/register/";
}

// =======================================================================================

var selectedDate = nextSaturday();
var selectedStartHour = 12;
var selectedEndHour = 24;

function updateDate() {
  var dateString = MONTHS[selectedDate.getMonth()] + " " + selectedDate.getDate();
  setText(document.getElementById("date"), dateString);
}

function updateStart() {
  setText(document.getElementById("start"), hourToString(selectedStartHour));
}

function updateEnd() {
  setText(document.getElementById("end"), hourToString(selectedEndHour));
}

function nextDate() {
  selectedDate = new Date(selectedDate.getTime() + 7 * 86400000);
  updateDate();
}

function prevDate() {
  selectedDate = new Date(selectedDate.getTime() - 7 * 86400000);
  updateDate();
}

function nextStart() {
  ++selectedStartHour;
  updateStart();
}

function prevStart() {
  --selectedStartHour;
  updateStart();
}

function nextEnd() {
  ++selectedEndHour;
  updateEnd();
}

function prevEnd() {
  --selectedEndHour;
  updateEnd();
}

function newEvent() {
  eventInfo = {
    startTime: selectedDate.getTime() + selectedStartHour * 3600000,
    endTime: selectedDate.getTime() + selectedEndHour * 3600000,
    theme: document.getElementById("theme").value,
    themeDescription: document.getElementById("themeDescription").value
  };
  
  post("/admin/api/new-event", eventInfo, function(response) {});
}

function setEventLocation() {
  request = {
    address: document.getElementById("address").value,
    mapsLink: document.getElementById("maps-link").value,
    phone: document.getElementById("phone").value
  };
  
  post("/admin/api/set-event-location", request, function(response) {});
}

function startupNewEventPage() {
  updateDate();
  updateStart();
  updateEnd();
}

//=======================================================================================

var chosenEvent = null;

function initializeRegistration(attendee) {
  document.getElementById("probability").value = attendee.probability;
  document.getElementById("duration").value = attendee.duration;
  document.getElementById("activity").value = attendee.activity;
  
  document.getElementById("name").value = attendee.name;
  document.getElementById("name-privacy").value = attendee.namePrivacy;
  
  for (var i in CONTRIB_LABELS) {
    document.getElementById("bring-" + CONTRIB_LABELS[i]).checked = false;
  }
  for (var i in attendee.contributions) {
    document.getElementById("bring-" + attendee.contributions[i]).checked = true;
  }
  
  document.getElementById("have-laptop").checked = attendee.haveLaptop;
  document.getElementById("notes").value = attendee.notes;
  
  setText(document.getElementById("submit"), "Update Registration");
  document.getElementById("cancel").style.display = "inline";
}

function clearRegistration() {
  document.getElementById("probability").value = "definitely";
  document.getElementById("duration").value = "attending";
  document.getElementById("activity").value = "playing";
  
  for (var i in CONTRIB_LABELS) {
    document.getElementById("bring-" + CONTRIB_LABELS[i]).checked = false;
  }
  
  document.getElementById("have-laptop").checked = false;
  document.getElementById("notes").value = "";
  
  setText(document.getElementById("submit"), "Register");
  document.getElementById("cancel").style.display = "none";
}

function makeApproveCallback(userId) {
  return function() {
    post("/admin/api/approve", {userId: userId}, function(response) {
      window.location.reload();
    });
  }
}

function chooseEvent(event) {
  chosenEvent = event;
  
  clearRegistration();
  
  // window.location.hash = event.key;
  
  var date = new Date(event.startTime);
  var endHour = (event.endTime - event.startTime) / 3600000 + date.getHours();
  setText(document.getElementById("event-date"), MONTHS[date.getMonth()] + " " + date.getDate());
  setText(document.getElementById("event-time"),
    hourToString(date.getHours()) + " to " + hourToString(endHour));
  
  setText(document.getElementById("event-theme"), event.theme ? event.theme : "(no theme)");
  setTextWithLineBreaks(document.getElementById("event-description"), 
      event.themeDescription ? event.themeDescription : "");
  
  var list = document.getElementById("event-attendees");
  while (list.childNodes.length > 0) {
    list.removeChild(list.childNodes[0]);
  }
  
  var anonymousAttendees = event.anonymousAttendees;
  for (var i in event.attendees) {
    var attendee = event.attendees[i];
    if (attendee.name && attendee.name != "") {
      var item = document.createElement("li");
      var name = document.createElement("span");
      name.className = attendee.isMe ? "roster-me" : "roster-name";
      setText(name, attendee.name);
      item.appendChild(name);
      
      var qualifiers = [];
      if (attendee.isMe) {
        qualifiers.push("you");
      }
      
      if (attendee.probability != "definitely") {
        qualifiers.push(attendee.probability);
      }
      if (attendee.duration == "droppingBy") {
        qualifiers.push("dropping by");
      }
      if (attendee.activity == "watching") {
        qualifiers.push("not playing");
      }
      
      for (var i in attendee.contributions) {
        qualifiers.push(attendee.contributions[i]);
      }
      if (attendee.haveLaptop) {
        qualifiers.push("laptop");
      }
      
      if ("email" in attendee) {
        qualifiers.push(attendee.email);
      }
      
      if (qualifiers.length > 0) {
        var element = document.createElement("span");
        element.className = "roster-qualifiers";
        setText(element, " (" + qualifiers.join("; ") + ")");
        item.appendChild(element);
      }
      
      if (("approved" in attendee) && !attendee.approved) {
        var approve = document.createElement("button");
        setText(approve, "approve");
        approve.onclick = makeApproveCallback(attendee.userId);
        item.appendChild(approve);
      }
      
      if (("notes" in attendee) && attendee.notes.trim() != "") {
        var notes = document.createElement("p");
        setTextWithLineBreaks(notes, attendee.notes);
        notes.className = "attendee-notes";
        item.appendChild(notes);
      }
      
      list.appendChild(item);
    } else {
      ++anonymousAttendees;
    }
    
    if (attendee.isMe) {
      initializeRegistration(attendee);
    }
  }
  if (anonymousAttendees > 0) {
    var item = document.createElement("li");
    setText(item, anonymousAttendees + " more");
    list.appendChild(item);
  }
}

function makeChooseEventCallback(event) {
  return function() {
    chooseEvent(event);
  }
}

function initializeRegistrationPage(response, key) {
  var userInfo = response.userInfo;
  document.getElementById("name").value = userInfo.name ? userInfo.name : "";
  setText(document.getElementById("email"), userInfo.email);
  document.getElementById("not-me").href = userInfo.logoutUrl;
  
  if (userInfo.approved) {
    document.getElementById("approval-warning").style.display = "none";
    if (userInfo.eventLocation) {
      var location = document.getElementById("location");
      removeChildren(location);
      var link = document.createElement("a");
      link.href = userInfo.eventLocation.mapsLink;
      setTextWithLineBreaks(link, userInfo.eventLocation.address);
      location.appendChild(link);
      location.appendChild(document.createElement("br"));
      location.appendChild(document.createElement("br"));
      location.appendChild(document.createTextNode(userInfo.eventLocation.phone));
      location.style.display = "block";
    }
  }
  
  if (!key) {
    key = window.location.hash;
    if (key && key.slice(0, 1) == "#") {
      key = key.slice(1);
    }
  }
  
  var table = document.getElementById("events");
  while (table.rows.length > 1) {
    table.deleteRow(1);
  }
  
  var events = response.events;
  var matchingEvent = null;
  var firstRadio = null;
  if (events.length > 0) {
    for (var i in events) {
      var row = table.insertRow(-1);
      var cell = row.insertCell(0);
      
      var radio = document.createElement("input");
      radio.type = "radio";
      radio.name = "event-radio";
      radio.onclick = makeChooseEventCallback(events[i]);
      if (i == 0) firstRadio = radio;
      cell.appendChild(radio);
      
      cell = row.insertCell(1);
      var date = new Date(events[i].startTime);
      setText(cell, MONTHS[date.getMonth()] + " " + date.getDate());
      
      cell = row.insertCell(2);
      setText(cell, events[i].theme ? events[i].theme : "(no theme)");
      
      cell = row.insertCell(3);
      setText(cell, events[i].attendees.length + events[i].anonymousAttendees);
      
      if (events[i].key == key) {
        matchingEvent = events[i];
        radio.checked = true;
      }
    }
  
    chooseEvent(matchingEvent ? matchingEvent : events[0]);
    if (!matchingEvent) firstRadio.checked = true;
    document.getElementById("content").style.display = "block";
  } else {
    document.getElementById("no-events").style.display = "block";
  }
}

function startupRegistrationPage() {
  document.body.className = isMobile ? "mobile" : "desktop";
  
  get("/register/api/register", function(response) {
    initializeRegistrationPage(response, null);
    document.getElementById("loading").style.display = "none";
    document.getElementById("done-loading").style.display = "block";
  });
}

var CONTRIB_LABELS = ["snacks", "beverages", "alcohol", "cash"];

function submitRegistration() {
  var attendee = {
    probability: document.getElementById("probability").value,
    duration: document.getElementById("duration").value,
    activity: document.getElementById("activity").value,
    name: document.getElementById("name").value.trim(),
    namePrivacy: document.getElementById("name-privacy").value,
    contributions: [],
    haveLaptop: document.getElementById("have-laptop").checked,
    notes: document.getElementById("notes").value
  };
  
  if (attendee.name == "") {
    alert("You forgot to fill in your name.");
    return;
  }
  
  for (var i in CONTRIB_LABELS) {
    if (document.getElementById("bring-" + CONTRIB_LABELS[i]).checked) {
      attendee.contributions.push(CONTRIB_LABELS[i]);
    }
  }
  
  var request = {
    attendee: attendee,
    eventKey: chosenEvent.key
  };
  
  var button = document.getElementById("submit");
  button.disabled = true;
  setText(button, "Sending...");
  document.getElementById("cancel").disabled = true;
  
  post("/register/api/register", request, function(response) {
    button.disabled = false;
    setText(button, "Register");
    document.getElementById("cancel").disabled = false;
    initializeRegistrationPage(response, chosenEvent.key);
  });
}

function cancelRegistration() {
  var request = {
    eventKey: chosenEvent.key
  };
  
  var button = document.getElementById("cancel");
  button.disabled = true;
  setText(button, "Cancelling...");
  document.getElementById("submit").disabled = true;
  
  post("/register/api/register", request, function(response) {
    button.disabled = false;
    setText(button, "Cancel Registration");
    document.getElementById("submit").disabled = false;
    initializeRegistrationPage(response, chosenEvent.key);
  });
}
