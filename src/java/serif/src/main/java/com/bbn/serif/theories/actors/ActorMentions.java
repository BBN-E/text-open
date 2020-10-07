package com.bbn.serif.theories.actors;

import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.PotentiallyAbsentSerifTheory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ActorMentions implements Iterable<ActorMention>, PotentiallyAbsentSerifTheory {

  private static final ActorMentions absent =
      new ActorMentions(Collections.<ActorMention>emptyList());

  //TODO: see where and how this method is used.
  @Override
  public boolean isAbsent() {
    return this == ActorMentions.absent();
  }

  public static ActorMentions absent() {
    return absent;
  }

  private ActorMentions(Iterable<ActorMention> actorMentions) {
    this.actorMentions = ImmutableList.copyOf(actorMentions);
  }

  public static ActorMentions create(Iterable<ActorMention> actorMentions) {
    return new ActorMentions(actorMentions);
  }

  public static ActorMentions createEmpty() {
    return new ActorMentions(ImmutableList.<ActorMention>of());
  }

  public int size() {
    return numActorMentions();
  }

  public ActorMention get(int idx) {
    return actorMention(idx);
  }

  public int numActorMentions() {
    return actorMentions.size();
  }

  public ActorMention actorMention(int idx) {
    return actorMentions.get(idx);
  }

  @Override
  public Iterator<ActorMention> iterator() {
    return actorMentions.iterator();
  }

  public List<ActorMention> asList() {
    return actorMentions;
  }

  public ImmutableSet<ActorMention> forMention(Mention m) {
    checkNotNull(m);
    final ImmutableSet.Builder<ActorMention> ret = ImmutableSet.builder();
    for (final ActorMention actorMention : actorMentions) {
      if (actorMention.mention() == m) {
        ret.add(actorMention);
      }
    }
    return ret.build();
  }

  private final List<ActorMention> actorMentions;
}
