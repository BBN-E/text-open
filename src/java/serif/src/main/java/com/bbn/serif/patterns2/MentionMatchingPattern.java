package com.bbn.serif.patterns2;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link Mention}s.
 */
@Beta
public interface MentionMatchingPattern extends Pattern {

  PatternReturns match(DocTheory dt, SentenceTheory st, Mention mention,
      PatternMatchState matchState, boolean fallThroughChildren);
}
