package com.bbn.serif.relations;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.relations.constraints.EventEventRelationMentionConstraint;
import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.theories.*;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Beta
public abstract class AbstractEventEventRelationMentionFinder implements EventEventRelationMentionFinder {

  private static final Logger log =
      LoggerFactory.getLogger(AbstractEventEventRelationMentionFinder.class);

  protected final boolean tolerant;

  protected AbstractEventEventRelationMentionFinder(final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public DocTheory addEventEventRelationMentions(final DocTheory docTheory,
      final Set<EventEventRelationMentionConstraint> constraints) {
    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();
    int count = 0;

    int previousNumberOfRelationMentions = docTheory.eventEventRelationMentions().asList().size();
    final EventEventRelationMentions rms =
            eventEventRelationMentions(docTheory, constraints);
    ret.eventEventRelationMentions(rms);

    count += rms.asList().size() - previousNumberOfRelationMentions;

    final DocTheory out = ret.build();
    for (final EventEventRelationMentionConstraint constraint : constraints) {
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

  protected abstract EventEventRelationMentions eventEventRelationMentions(
      final DocTheory docTheory,
      final Set<EventEventRelationMentionConstraint> constraints);
}

