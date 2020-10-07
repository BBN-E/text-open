package com.bbn.serif.theories;

import com.bbn.serif.theories.PotentiallyAbsentSerifTheory;
import com.bbn.serif.theories.EventEventRelationMention;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class EventEventRelationMentions implements Iterable<EventEventRelationMention>, PotentiallyAbsentSerifTheory {

  private static EventEventRelationMentions absent = new EventEventRelationMentions(Collections.<EventEventRelationMention>emptyList());

  public static EventEventRelationMentions absent() {
    return absent;
  }

  @Override
  public boolean isAbsent() {
    return this == EventEventRelationMentions.absent();
  }

  private EventEventRelationMentions(Iterable<EventEventRelationMention> EventEventRelationMentions) {
    this.eventEventRelationMentions = ImmutableList.copyOf(EventEventRelationMentions);
  }

  public static EventEventRelationMentions create(Iterable<EventEventRelationMention> EventEventRelationMentions) {
    return new EventEventRelationMentions(EventEventRelationMentions);
  }

  public static EventEventRelationMentions createEmpty() {
    return create(ImmutableSet.<EventEventRelationMention>of());
  }

  @Override
  public Iterator<EventEventRelationMention> iterator() {
    return eventEventRelationMentions.iterator();
  }

  public List<EventEventRelationMention> asList() {
    return eventEventRelationMentions;
  }

  public int numEventEventRelationMentions() {
    return eventEventRelationMentions.size();
  }

  public EventEventRelationMention EventEventRelationMention(int idx) {
    return eventEventRelationMentions.get(idx);
  }

  private final List<EventEventRelationMention> eventEventRelationMentions;

}
