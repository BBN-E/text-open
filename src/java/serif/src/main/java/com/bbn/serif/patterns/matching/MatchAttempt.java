package com.bbn.serif.patterns.matching;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.patterns.Pattern;

import com.google.common.annotations.Beta;

import org.immutables.value.Value;

/**
 * Represents an attempt to match {@link #pattern()}  against {@link #toMatch()}. This is used
 * for pattern match caching in {@link PatternMatchState}.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
abstract class MatchAttempt {
  public abstract Pattern pattern();
  public abstract Object toMatch();

  public static MatchAttempt of(Pattern pattern, Object toMatch) {
    return ImmutableMatchAttempt.builder().pattern(pattern)
        .toMatch(toMatch).build();
  }
}
