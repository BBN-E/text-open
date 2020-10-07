package com.bbn.serif.coreference.representativementions;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;

/**
 * Finds a representative mention for an entity, where some mention within the entity is given
 * special consideration in some way specific to the implementation.
 */
public interface FocusedRepresentativeMentionStrategy {

  public Entity.RepresentativeMention representativeMentionForEntity(final Entity e,
      final Mention m);
}
