package com.bbn.serif.patterns.matching;

import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link SentenceTheory}s.
 */
@Beta
public interface SentenceMatchingPattern {
    PatternReturns match(DocTheory dt, SentenceTheory st,
                         PatternMatchState matchState, boolean fallThroughChildren);
}
