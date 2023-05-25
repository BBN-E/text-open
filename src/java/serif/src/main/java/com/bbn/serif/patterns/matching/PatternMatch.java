package com.bbn.serif.patterns.matching;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.patterns.Pattern;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

/**
 * A match of a {@link Pattern} against a Serif object.  Either a {@link SentenceTheory} or a
 * {@link Spanning} is guaranteed to be present.
 */
@Beta
public interface PatternMatch {

  Optional<Pattern> pattern();

  DocTheory docTheory();

  Optional<SentenceTheory> sentenceTheory();

  Optional<Spanning> spanning();
}
