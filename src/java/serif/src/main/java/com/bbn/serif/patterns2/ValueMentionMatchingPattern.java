package com.bbn.serif.patterns2;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMention;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link ValueMention}s.
 */
@Beta
public interface ValueMentionMatchingPattern extends Pattern {

  PatternReturns match(DocTheory dt, SentenceTheory st, ValueMention mention,
      PatternMatchState matchState);
}
