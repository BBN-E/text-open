package com.bbn.serif.mentions;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.mentions.constraints.MentionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mentions;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Beta
public abstract class AbstractSentenceMentionFinder implements MentionFinder {

  private static final Logger log = LoggerFactory.getLogger(AbstractSentenceMentionFinder.class);

  protected final boolean tolerant;

  protected AbstractSentenceMentionFinder(@TolerantP final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public DocTheory addMentions(final DocTheory docTheory,
      final Set<MentionConstraint> constraints) {
    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();

    int count = 0;
    for (int i = 0; i < docTheory.numSentences(); ++i) {
      final SentenceTheory st = docTheory.sentenceTheory(i);
      final SentenceTheory newSt =
          st.modifiedCopyBuilder().mentions(mentionsForSentence(docTheory, st, constraints))
              .build();
      ret.replacePrimarySentenceTheory(st, newSt);
      count += newSt.mentions().size();
    }

    final DocTheory out = ret.build();
    for (final MentionConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.warn("Constraint " + constraint + " violated by " + out);
        } else {
          throw new SerifException("Constraint " + constraint + " violated by " + out);
        }
      }
    }
    log.info("for {} turned {} constraints into {} mentions", docTheory.docid(),
        ImmutableList.copyOf(constraints).size(), count);
    return out;
  }

  protected abstract Mentions mentionsForSentence(final DocTheory docTheory,
      final SentenceTheory st,
      final Set<MentionConstraint> constraints);

  @Override
  public void finish() {
    // do nothing
  }
}
