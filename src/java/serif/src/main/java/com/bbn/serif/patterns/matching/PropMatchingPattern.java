package com.bbn.serif.patterns.matching;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.patterns.Pattern;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link Proposition}s.
 */
@Beta
public interface PropMatchingPattern {

  PatternReturns match(DocTheory dt, SentenceTheory st, Proposition proposition,
      PatternMatchState matchState, boolean fallThroughChildren);
}
