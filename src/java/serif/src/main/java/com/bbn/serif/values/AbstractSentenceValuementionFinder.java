package com.bbn.serif.values;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMentions;
import com.bbn.serif.values.constraints.ValueMentionConstraint;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Beta
public abstract class AbstractSentenceValuementionFinder implements ValueMentionFinder {

  private static final Logger log = LoggerFactory.getLogger(AbstractSentenceValuementionFinder.class);

  protected final boolean tolerant;

  protected AbstractSentenceValuementionFinder(final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public DocTheory addValues(final DocTheory input,
      final Set<ValueMentionConstraint> constraints) {
    final DocTheory.Builder ret = input.modifiedCopyBuilder();
    int count = 0;
    for (int i = 0; i < input.numSentences(); i++) {
      final SentenceTheory sentToReplace = input.sentenceTheory(i);
      final ValueMentions vms = valueMentionsForSentence(sentToReplace, constraints);
      count += vms.size();
      ret.replacePrimarySentenceTheory(sentToReplace, sentToReplace.modifiedCopyBuilder()
          .valueMentions(vms)
          .build());
    }
    final DocTheory out = ret.build();
    for (final ValueMentionConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.warn("Constraint " + constraint + " violated by " + out);
        } else {
          throw new SerifException("Constraint " + constraint + " violated by " + out);
        }
      }
    }
    log.debug("found {} ValueMentions for {} with {} constraints", count,
        input.docid(),
        ImmutableList.copyOf(constraints).size());
    return out;
  }

  protected abstract ValueMentions valueMentionsForSentence(final SentenceTheory sentenceTheory,
      final Set<ValueMentionConstraint> constraints);
}
