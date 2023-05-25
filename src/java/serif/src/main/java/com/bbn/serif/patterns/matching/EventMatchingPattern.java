package com.bbn.serif.patterns.matching;

import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link EventMention}s.
 */
@Beta
public interface EventMatchingPattern {
    PatternReturns match(DocTheory dt, SentenceTheory st, EventMention eventMention,
                         PatternMatchState matchState, boolean fallThroughChildren);
}
