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
public abstract class AbstractConstraintlessSentenceEventMentionFinder implements EventMentionFinder {

  private static final Logger log = LoggerFactory.getLogger(AbstractConstraintlessSentenceEventMentionFinder.class);

  protected final boolean tolerant;

  protected AbstractConstraintlessSentenceEventMentionFinder() {
    this.tolerant = false;
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
          .eventMentions(eventMentionsFromNoConstraints(input, originalSt))
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

  @Override
  public void finish() {
    // do nothing
  }

  protected abstract EventMentions eventMentionsFromNoConstraints(final DocTheory docTheory,
                                                                final SentenceTheory sentenceTheory);
}
