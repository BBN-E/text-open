package com.bbn.serif.relations;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.Relations;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Beta
public abstract class AbstractSentenceRelationMentionFinder implements RelationMentionFinder {

  private static final Logger log =
      LoggerFactory.getLogger(AbstractSentenceRelationMentionFinder.class);

  protected final boolean tolerant;

  protected AbstractSentenceRelationMentionFinder(final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public DocTheory addRelationMentions(final DocTheory docTheory,
      final Set<RelationMentionConstraint> constraints) {
    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();
    int count = 0;
    for (int i = 0; i < docTheory.numSentences(); i++) {
      final SentenceTheory st = docTheory.sentenceTheory(i);
      int previousNumberOfRelationMentions = st.relationMentions().size();
      final RelationMentions rms =
          relationMentionsForSentence(docTheory, st, constraints);
      ret.replacePrimarySentenceTheory(st, st.modifiedCopyBuilder()
          .relationMentions(rms).build());
      count += rms.size() - previousNumberOfRelationMentions;
    }


    // Bonan: wipe out all relations
    ret.relations(Relations.absent());
    //


    final DocTheory out = ret.build();
    for (final RelationMentionConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.warn("Constraint " + constraint + " violated by " + out);
        } else {
          throw new SerifException("Constraint " + constraint + " violated by " + out);
        }
      }
    }
    log.debug("Added net {} relation mentions to {} with {} constraints", count, docTheory.docid(),
        ImmutableList.copyOf(constraints).size());
    return out;
  }

  @Override
  public void finish() {
    // do nothing
  }

  protected abstract RelationMentions relationMentionsForSentence(
      final DocTheory docTheory, final SentenceTheory sentenceTheory,
      final Set<RelationMentionConstraint> constraints);
}

