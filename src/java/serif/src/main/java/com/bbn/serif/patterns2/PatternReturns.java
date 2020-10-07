package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

/**
 * Represents the result of matching a {@link Pattern} against something.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class PatternReturns {

  public abstract ImmutableSet<PatternMatch> matches();

  public final boolean matched() {
    return !matches().isEmpty();
  }

  private static final PatternReturns NO_MATCHES = new Builder().build();

  public static PatternReturns noMatches() {
    return NO_MATCHES;
  }

  public static PatternReturns of(Iterable<? extends PatternMatch> patternMatches) {
    return new Builder().addAllMatches(patternMatches).build();
  }

  public static PatternReturns of(final PatternMatch match) {
    return new Builder().addMatches(match).build();
  }

  public static class Builder extends ImmutablePatternReturns.Builder {

    public void addAll(PatternReturns other) {
      addAllMatches(other.matches());
    }
  }
}
