package com.bbn.serif.patterns2;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against entire {@link SentenceTheory}s.
 */
@Beta
public interface SentenceMatchingPattern extends Pattern {

  PatternReturns match(DocTheory dt, SentenceTheory st,
      PatternMatchState matchState);
}
