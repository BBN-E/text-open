package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

/**
 * A collection of patterns meant to be matched together. They may refer to one another's results
 * and to labels applied to entities by other patterns.  These should be matched using a
 * {@link PatternSetMatcher}.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class PatternSet {

  public abstract ImmutableSet<Pattern> patterns();

  public abstract ImmutableSet<EntityPattern> entityPatterns();

  public static PatternSet of(Iterable<? extends Pattern> patterns) {
    return ImmutablePatternSet.builder().patterns(patterns).build();
  }
}

