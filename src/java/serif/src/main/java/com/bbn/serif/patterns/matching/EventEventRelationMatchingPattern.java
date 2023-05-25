package com.bbn.serif.patterns.matching;

import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventEventRelationMention;
import com.google.common.annotations.Beta;

/**
 * A {@link Pattern} which can match against {@link EventEventRelationMention}s.
 */
@Beta
public interface EventEventRelationMatchingPattern {
    PatternReturns match(DocTheory dt, EventEventRelationMention eer,
                         PatternMatchState matchState, boolean fallThroughChildren);
}
