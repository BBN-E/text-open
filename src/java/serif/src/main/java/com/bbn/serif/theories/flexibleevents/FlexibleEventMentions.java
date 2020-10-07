package com.bbn.serif.theories.flexibleevents;

import com.bbn.serif.theories.PotentiallyAbsentSerifTheory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Iterator;

public final class FlexibleEventMentions implements Iterable<FlexibleEventMention>,
    PotentiallyAbsentSerifTheory {

  private final ImmutableList<FlexibleEventMention> flexibleEventMentions;
  private final boolean isAbsent;

  private FlexibleEventMentions(
      final Iterable<FlexibleEventMention> flexibleEventMentions, final boolean isAbsent) {
    this.flexibleEventMentions = ImmutableList.copyOf(flexibleEventMentions);
    this.isAbsent = isAbsent;
  }

  public ImmutableList<FlexibleEventMention> flexibleEventMentions() {
    return flexibleEventMentions;
  }

  public FlexibleEventMention get(int index) {
    Preconditions.checkElementIndex(index, flexibleEventMentions.size());
    return flexibleEventMentions.get(index);
  }

  public int size() {
    return flexibleEventMentions.size();
  }

  @Override
  public boolean isAbsent() { return isAbsent; }

  @Override
  public Iterator<FlexibleEventMention> iterator() {
    return flexibleEventMentions.iterator();
  }

  public static FlexibleEventMentions absent() {
    return new FlexibleEventMentions(ImmutableSet.<FlexibleEventMention>of(), true);
  }

  public static FlexibleEventMentions createEmpty() {
    return new FlexibleEventMentions(ImmutableSet.<FlexibleEventMention>of(), false);
  }

  public static FlexibleEventMentions from(final Iterable<FlexibleEventMention> flexibleEventMentions) {
    return new FlexibleEventMentions(flexibleEventMentions, false);
  }
}
