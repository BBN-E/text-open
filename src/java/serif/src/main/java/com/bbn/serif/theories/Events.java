package com.bbn.serif.theories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Events implements Iterable<Event>, PotentiallyAbsentSerifTheory {

  private static Events absent = new Events(Collections.<Event>emptyList());

  @Override
  public boolean isAbsent() {
    return this == Events.absent();
  }

  public static Events absent() {
    return absent;
  }

  public static Events create(Iterable<Event> events) {
    return new Events(events);
  }

  public static Events createEmpty() {
    return new Events(ImmutableSet.<Event>of());
  }

  /**
   * Not really deprecated, just not to be used as a public constructor
   */
  public Events(Iterable<Event> events) {
    this.events = ImmutableList.copyOf(events);
  }

  @Override
  public Iterator<Event> iterator() {
    return events.iterator();
  }

  public List<Event> asList() {
    return events;
  }

  public int numEvents() {
    return events.size();
  }

  public Event event(int idx) {
    return events.get(idx);
  }

  private final List<Event> events;
}

