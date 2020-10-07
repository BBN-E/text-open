package com.bbn.serif.entities;


import com.bbn.serif.entities.constraints.EntityConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The NoOpEntityFinder does not take and will not enforce constraints
 */
@Beta
public final class NoOpEntityFinder implements EntityFinder {

  @Inject
  private NoOpEntityFinder() {

  }

  public static NoOpEntityFinder create() {
    return new NoOpEntityFinder();
  }

  @Override
  public DocTheory addEntities(final DocTheory input,
      final Set<EntityConstraint> constraints) {
    checkArgument(ImmutableList.copyOf(constraints.iterator()).size() == 0,
        "NoOpEntityFinder cannot handle constraints");
    return input;
  }

  @Override
  public void finish() throws IOException {
  }
}
