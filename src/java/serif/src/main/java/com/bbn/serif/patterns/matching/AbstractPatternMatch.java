package com.bbn.serif.patterns.matching;

import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Convenience class for implementers of {@link PatternMatch}. {@link PatternMatch}
 * {@code check} methods should call {@code super.check()} to ensure
 * pattern match preconditions are satisfied.
 */
@Beta
abstract class AbstractPatternMatch implements PatternMatch {

  protected void check() {
    checkArgument(sentenceTheory().isPresent() || spanning().isPresent());
  }
}
