package com.bbn.serif.theories.icewseventmentions;

import com.bbn.serif.theories.PotentiallyAbsentSerifTheory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;

public final class ICEWSEventMentions
    implements Iterable<ICEWSEventMention>, PotentiallyAbsentSerifTheory {

  private static ICEWSEventMentions absent =
      new ICEWSEventMentions(Collections.<ICEWSEventMention>emptyList());

  @Override
  public boolean isAbsent() {
    return this == ICEWSEventMentions.absent();
  }

  public static ICEWSEventMentions absent() {
    return absent;
  }

  private ICEWSEventMentions(Iterable<ICEWSEventMention> icewsEventMentions) {
    this.icewsEventMentions = ImmutableList.copyOf(icewsEventMentions);
  }

  public static ICEWSEventMentions create(Iterable<ICEWSEventMention> icewsEventMentions) {
    return new ICEWSEventMentions(icewsEventMentions);
  }

  public int numICEWSEventMentions() {
    return icewsEventMentions.size();
  }

  public ICEWSEventMention icewsEventMention(int idx) {
    return icewsEventMentions.get(idx);
  }

  public int size() {
    return numICEWSEventMentions();
  }

  public ICEWSEventMention get(int idx) {
    return icewsEventMention(idx);
  }

  @Override
  public Iterator<ICEWSEventMention> iterator() {
    return icewsEventMentions.iterator();
  }

  public ImmutableList<ICEWSEventMention> asList() {
    return icewsEventMentions;
  }

  private final ImmutableList<ICEWSEventMention> icewsEventMentions;

  public static ICEWSEventMentions createEmpty() {
    return new ICEWSEventMentions(ImmutableSet.<ICEWSEventMention>of());
  }
}
