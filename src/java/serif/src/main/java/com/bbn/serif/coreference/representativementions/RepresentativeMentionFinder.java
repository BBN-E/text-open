package com.bbn.serif.coreference.representativementions;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Entity.RepresentativeMention;

import com.google.common.annotations.Beta;

/**
 * Finds the most representative mention for an entity.
 *
 * @author rgabbard
 */
@Beta
public interface RepresentativeMentionFinder {

  /**
   * Gets the most representative mention for this entity according to this strategy.
   * Implementations should ensure the returned mention is deterministic.
   */
  public RepresentativeMention representativeMentionForEntity(Entity e);
}
