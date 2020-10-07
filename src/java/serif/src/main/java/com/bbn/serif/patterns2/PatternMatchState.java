package com.bbn.serif.patterns2;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Entity;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

/**
 * Tracks all the mutable state during matching a {@link PatternSet} against
 * a {@link com.bbn.serif.theories.DocTheory}. In particular, this tracks entity-level matches
 * and caches sub-pattern matches for increased performance.
 */
@Beta
final class PatternMatchState {
  private final ListMultimap<Entity, Symbol> labelsForEntity = ArrayListMultimap.create();
  private final Set<MatchAttempt> unmatchedCache = Sets.newHashSet();
  private final Map<MatchAttempt, PatternReturns> matchCache = Maps.newHashMap();

  private PatternMatchState() {}

  public static PatternMatchState create() {
    return new PatternMatchState();
  }

  /**
   * Caches that the result of matching {@code pattern} against {@code object} is {@code matches}.
   */
  public PatternReturns registerPatternMatch(Pattern pattern, Object object, Set<PatternMatch> matches) {
    final PatternReturns ret = PatternReturns.of(matches);
    matchCache.put(MatchAttempt.of(pattern, object), ret);
    return ret;
  }

  /**
   * Caches that the result of matching {@code pattern} against {@code object} is {@code match}.
   */
  public PatternReturns registerPatternMatch(Pattern pattern, Object object, PatternMatch match) {
    final PatternReturns ret = PatternReturns.of(match);
    matchCache.put(MatchAttempt.of(pattern, object), ret);
    return ret;
  }

  /**
   * Caches that {@code pattern} does not match {@code object}
   */
  public PatternReturns registerUnmatched(Pattern pattern, Object object) {
    unmatchedCache.add(MatchAttempt.of(pattern, object));
    return PatternReturns.noMatches();
  }

  /**
   * Gets the cached result of applying {@code pattern} to {@code object}, if available.
   */
  public Optional<PatternReturns> cachedMatches(Pattern pattern, Object object) {
    final MatchAttempt matchAttempt = MatchAttempt.of(pattern, object);
    if (unmatchedCache.contains(matchAttempt)) {
      return Optional.of(PatternReturns.noMatches());
    }

    final PatternReturns cached = matchCache.get(matchAttempt);
    if (cached != null) {
      return Optional.of(cached);
    } else {
      return Optional.absent();
    }
  }

  public Set<Symbol> labelsForEntity(Entity e) {
    return ImmutableSet.copyOf(labelsForEntity.get(e));
  }
}
