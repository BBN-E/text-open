package com.bbn.serif.relations;

import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.SentenceTheory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AbstractConstraintlessSentenceRelationMentionFinder
    extends AbstractSentenceRelationMentionFinder {

  protected AbstractConstraintlessSentenceRelationMentionFinder() {
    super(false);
  }

  @Override
  protected RelationMentions relationMentionsForSentence(
      final DocTheory docTheory, final SentenceTheory sentenceTheory,
      final Set<RelationMentionConstraint> constraints) {
    checkArgument(constraints.isEmpty(), "Constraints not supported");
    return relationMentionsForSentence(docTheory, sentenceTheory);
  }

  protected abstract RelationMentions relationMentionsForSentence(final DocTheory docTheory,
      final SentenceTheory sentenceTheory);
}
