package com.bbn.serif.patterns.matching;

import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link EventEventRelationMention.Argument}s.
 */
@Beta
public interface EventArgumentMatchingPattern {
    PatternReturns match(DocTheory dt, SentenceTheory st, EventEventRelationMention.Argument a,
                         PatternMatchState matchState, boolean fallThroughChildren);
}
