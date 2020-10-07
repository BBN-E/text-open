package com.bbn.serif.theories;

import com.bbn.serif.types.EntityType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Entities implements Iterable<Entity>, PotentiallyAbsentSerifTheory {

  public Entity entity(int idx) {
    return entities.get(idx);
  }

  public int numEntities() {
    return entities.size();
  }

  private String score;

  public List<Entity> asList() {
    return entities;
  }

  private static Entities absent = new Entities(Collections.<Entity>emptyList(), "0");

  @Override
  public boolean isAbsent() {
    return this == Entities.absent();
  }

  public static Entities absent() {
    return absent;
  }

  public int numEntitiesOfType(EntityType type) {
    return entitiesByType.get(type).size();
  }

  public int size() {
    return numEntities();
  }

  public Entity get(int idx) {
    return entity(idx);
  }

  public String score() {
    return score;
  }

  public Optional<Entity> entityByMention(Mention searchMention) {
    for (final Entity e : entitiesByType.get(searchMention.entityType())) {
      for (final Mention m : e) {
        if (m == searchMention) {
          return Optional.of(e);
        }
      }
    }

    return Optional.absent();
  }

  public Optional<Entity> entityByMention(Mention searchMention,
      EntityType type) {
    for (final Entity e : entitiesByType.get(type)) {
      for (final Mention m : e) {
        if (m == searchMention) {
          return Optional.of(e);
        }
      }
    }

    return Optional.absent();
  }

  public Map<Mention, Entity> createMentionToEntityMap() {
    final Map<Mention, Entity> map = Maps.newHashMap();
    for (final Entity entity : this) {
      for (final Mention ment : entity) {
        map.put(ment, entity);
      }
    }
    return map;
  }

  private final List<Entity> entities;
  private final Multimap<EntityType, Entity> entitiesByType;

  public static Entities create(Iterable<Entity> entities, String score) {
    return new Entities(entities, score);
  }

  /**
   * Returns an empty set of entities with score 1.0
   */
  public static Entities createEmpty() {
    return new Entities(ImmutableList.<Entity>of(), "1.0");
  }

  /**
   * Not really deprecated, but its use as a public method is
   */
  @Deprecated
  public Entities(Iterable<Entity> entities, String score) {
    this.entities = ImmutableList.copyOf(entities);
    this.entitiesByType = Multimaps.index(this, Entity.EntityType);
    this.score = score;
  }

  @Override
  public Iterator<Entity> iterator() {
    return entities.iterator();
  }


}
