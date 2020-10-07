package com.bbn.serif.relations;

import com.bbn.serif.relations.constraints.RelationConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@Beta
public class NoOpRelationFinder implements RelationFinder {

  @Override
  public DocTheory addRelations(final DocTheory docTheory,
      final Set<RelationConstraint> constraints) {
    checkArgument(ImmutableList.copyOf(constraints.iterator()).size() == 0,
        "NoOpRelationFinder doesn't know how to handle constraints");
    return docTheory;
  }

  @Override
  public void finish() {

  }
}
