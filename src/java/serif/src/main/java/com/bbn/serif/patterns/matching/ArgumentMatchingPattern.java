package com.bbn.serif.patterns.matching;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.patterns.Pattern;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link Mention}s.
 */
@Beta
public interface ArgumentMatchingPattern {

  PatternReturns match(DocTheory dt, SentenceTheory st, Proposition.Argument a,
      PatternMatchState matchState, boolean fallThroughChildren);
}
