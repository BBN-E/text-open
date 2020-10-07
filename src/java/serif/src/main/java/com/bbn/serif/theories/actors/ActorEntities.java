package com.bbn.serif.theories.actors;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.PotentiallyAbsentSerifTheory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ActorEntities implements Iterable<ActorEntity>, PotentiallyAbsentSerifTheory {

  private static ActorEntities absent = new ActorEntities(Collections.<ActorEntity>emptyList());

  @Override
  public boolean isAbsent() {
    return this == ActorEntities.absent();
  }

  public static ActorEntities absent() {
    return absent;
  }

  private ActorEntities(Iterable<ActorEntity> actorEntities) {
    this.actorEntities = ImmutableList.copyOf(actorEntities);
  }

  public static ActorEntities create(Iterable<ActorEntity> actorEntities) {
    return new ActorEntities(actorEntities);
  }

  public static ActorEntities createEmpty() {
    return create(ImmutableSet.<ActorEntity>of());
  }

  @Override
  public Iterator<ActorEntity> iterator() {
    return actorEntities.iterator();
  }

  public List<ActorEntity> asList() {
    return actorEntities;
  }

  public int numActorEntities() {
    return actorEntities.size();
  }

  public ActorEntity actorEntity(int idx) {
    return actorEntities.get(idx);
  }

  public ImmutableSet<ActorEntity> forEntity(Entity e) {
    checkNotNull(e);
    final ImmutableSet.Builder<ActorEntity> ret = ImmutableSet.builder();

    for (final ActorEntity actorEntity : actorEntities) {
      if (actorEntity.entity() == e) {
        ret.add(actorEntity);
      }
    }

    return ret.build();
  }

  private final List<ActorEntity> actorEntities;
}
