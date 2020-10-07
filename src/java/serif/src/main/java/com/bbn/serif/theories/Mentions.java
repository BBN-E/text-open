package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.common.SerifException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import org.immutables.func.Functional;

import java.util.Iterator;
import java.util.List;

import static com.bbn.bue.common.strings.offsets.OffsetRange.containedInPredicate;
import static com.bbn.serif.theories.Spannings.toCharOffsetRange;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.compose;

@JsonSerialize(as = ImmutableMentions.class)
@JsonDeserialize(as = ImmutableMentions.class)
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@Functional
public abstract class Mentions
    implements Iterable<Mention>, PotentiallyAbsentSerifTheory, WithMentions {

  public abstract ImmutableSet<Mention> mentions();

  /**
   * {@code parse} will always be present except when {@link #isAbsent()} is {@code true}
   */
  public abstract Optional<Parse> parse();

  @org.immutables.value.Value.Default
  public float descScore() {
    return 1.0f;
  }

  @org.immutables.value.Value.Default
  public float nameScore() {
    return 1.0f;
  }

  @org.immutables.value.Value.Default
  @Override
  public boolean isAbsent() {
    return false;
  }

  @org.immutables.value.Value.Check
  protected void check() {
    checkArgument(!isAbsent() || mentions().isEmpty(),
        "Absent Mentions must have not mentions");
    checkArgument(isAbsent() || (parse().isPresent()?!parse().get().isAbsent():true));
  }

  public static Mentions absent() {
    return new Builder()
        .isAbsent(true)
        .build();
  }

  public final Mention mention(int index) {
    return mentions().asList().get(index);
  }

  public final int numMentions() {
    return mentions().size();
  }

  public final int size() {
    return numMentions();
  }

  public final Mention get(int idx) {
    return mention(idx);
  }

  @Override
  public final Iterator<Mention> iterator() {
    return mentions().iterator();
  }

  public final List<Mention> asList() {
    return mentions().asList();
  }

  /**
   * Returns the first mention (by token index, ties broken by length) in the provided offset
   * range. The boolean arg preferLongerLengths determines whether longer length or shorter length
   * is preferred in ties.
   *
   * @deprecated Prefer {@link #bestMentionInRange(OffsetRange, Predicate, Ordering)}
   */
  @Deprecated
  public final Optional<Mention> firstMentionInRange(
      final OffsetRange<CharOffset> charOffsetOffsetRange, final Predicate<Mention> mentionFilter) {
    Mention ret = null;
    for (final Mention mention : mentions()) {
      if (charOffsetOffsetRange
          .contains(mention.span().charOffsetRange())) {  // We are inside the range
        if (ret == null ||  // We haven't found anything yet
            // We start earlier than the current best
            mention.span().startTokenIndexInclusive() < ret.span().startTokenIndexInclusive() ||
            // We share the current best start
            (mention.span().startTokenIndexInclusive() == ret.span().startTokenIndexInclusive() &&
                 mention.span().endTokenIndexInclusive() < ret.span().endTokenIndexInclusive())) {
          if (mentionFilter.apply(mention)) {  // Make sure we satisfy our predicate
            ret = mention;
          }
        }
      }
    }
    return Optional.fromNullable(ret);
  }

  /**
   * Gets the mention, if any, contained (by character offsets) within {@code containingRange}
   * which passes {@code mentionFilter}.  If more than one meets those criterion, the one which
   * is best (latest in sort order) according to {@code sortOrder} is used.
   */
  public Optional<Mention> bestMentionInRange(
      OffsetRange<CharOffset> containingRange, Predicate<Mention> mentionFilter,
      Ordering<? super Mention> sortOrder) {
    final ImmutableSet<Mention> candidates = FluentIterable.from(this.mentions())
        .filter(mentionFilter)
        .filter(compose(containedInPredicate(containingRange), toCharOffsetRange()))
        .toSet();
    if (!candidates.isEmpty()) {
      return Optional.of(sortOrder.max(candidates));
    } else {
      return Optional.absent();
    }
  }

  @SuppressWarnings("deprecation")
  public final Optional<Mention> longestRecognizedMentionBeginningAt(Token tok) {
    Mention bestMention = null;
    int bestSize = 0;

    for (final Mention m : this) {
      if (m.isOfRecognizedType() && m.span().startIndex() == tok.index()) {
        if (m.span().size() > bestSize) {
          bestMention = m;
          bestSize = m.span().size();
        }
      }
    }
    return Optional.fromNullable(bestMention);
  }

  /**
   * Returns the smallest mention which covers the provided range of character offsets, if any.
   * Smallest is defined in terms of number of tokens.
   */
  public final Optional<Mention> smallestMentionCovering(
      OffsetRange<CharOffset> charOffsetOffsetRange) {
    Mention ret = null;

    for (final Mention mention : mentions()) {
      if (mention.span().charOffsetRange().contains(charOffsetOffsetRange)) {
        if (ret == null || mention.span().size() < ret.span().size()) {
          ret = mention;
        }
      }
    }

    return Optional.fromNullable(ret);
  }

  /**
   * Returns the smallest mention which a) covers the provided range of character offsets, and b)
   * satisfies the provided Predicate.  If no such Mention exists, Optional.absent() is returned.
   * Smallest is defined in terms of number of tokens.
   */
  public final Optional<Mention> smallestMentionCovering(
      final OffsetRange<CharOffset> charOffsetOffsetRange, final Predicate<Mention> mentionFilter) {
    Mention ret = null;
    for (final Mention mention : mentions()) {
      if (mention.span().charOffsetRange().contains(charOffsetOffsetRange)) {
        if (ret == null || mention.span().size() < ret.span().size()) {
          if (mentionFilter.apply(mention)) {  // Make sure we satisfy our predicate
            ret = mention;
          }
        }
      }
    }
    return Optional.fromNullable(ret);
  }




  public static class Builder extends ImmutableMentions.Builder {

  }
}
