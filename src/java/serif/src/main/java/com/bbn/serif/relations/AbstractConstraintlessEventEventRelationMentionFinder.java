package com.bbn.serif.relations;

import com.bbn.serif.relations.constraints.EventEventRelationMentionConstraint;
import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventEventRelationMentions;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.SentenceTheory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AbstractConstraintlessEventEventRelationMentionFinder
    extends AbstractEventEventRelationMentionFinder {

  protected AbstractConstraintlessEventEventRelationMentionFinder() {
    super(false);
  }

  @Override
  protected EventEventRelationMentions eventEventRelationMentions(
      final DocTheory docTheory,
      final Set<EventEventRelationMentionConstraint> constraints) {
    checkArgument(constraints.isEmpty(), "Constraints not supported");
    return eventEventRelationMentions(docTheory);
  }

  protected abstract EventEventRelationMentions eventEventRelationMentions(final DocTheory docTheory);
}
