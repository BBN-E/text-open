package com.bbn.serif.events;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.events.constraints.EventMentionConstraint;
import com.bbn.serif.mentions.MentionFinder;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMentions;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Beta
public abstract class AbstractSentenceEventMentionFinder implements EventMentionFinder {

  private static final Logger log = LoggerFactory.getLogger(AbstractSentenceEventMentionFinder.class);

  protected final boolean tolerant;

  protected AbstractSentenceEventMentionFinder(@MentionFinder.TolerantP final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public DocTheory addEventMentions(final DocTheory input,
      final Set<EventMentionConstraint> constraints) {
    final DocTheory.Builder ret = input.modifiedCopyBuilder();
    int count = 0;
    for (int i = 0; i < input.numSentences(); i++) {
      // TODO restrict constraints to only relevant sentence theories, perhaps via offsets?
      final SentenceTheory originalSt = input.sentenceTheory(i);
      final SentenceTheory newSt = originalSt.modifiedCopyBuilder()
          .eventMentions(eventMentionsFromConstraints(originalSt, constraints))
          .build();
      count += newSt.eventMentions().size();
      ret.replacePrimarySentenceTheory(originalSt, newSt);
    }

    final DocTheory out = ret.build();
    for (final EventMentionConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.warn("Constraint " + constraint + " violated by " + out);
        } else {
          throw new SerifException("Constraint " + constraint + " violated by " + out);
        }
      }
    }
    log.info("for {} turned {} constraints into {} event mentions", input.docid(),
        ImmutableList.copyOf(constraints).size(), count);
    return out;
  }

  protected abstract EventMentions eventMentionsFromConstraints(final SentenceTheory sentenceTheory,
      final Set<EventMentionConstraint> constraints);
}
