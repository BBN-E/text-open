package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.collections.MultimapUtils;
import com.bbn.bue.common.strings.offsets.TokenOffset;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableRangeSet;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.TreeRangeSet;

import org.immutables.value.Value;

import java.util.Iterator;

import static com.bbn.bue.common.OptionalUtils.deoptionalizeFunction;
import static com.bbn.bue.common.OrderingUtils.maxFunction;
import static com.google.common.base.Functions.compose;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * An assignment of names to the tokens of a sentence.
 *
 * Note that a {@link Names} score is included in its equality and hashCode calculations for now.
 * This is sad and we may revisit this in the future. For the moment we say a {@link Names} is
 * in <i>canonical form</i> if it contains no {@link Name}s which differ only in score.  You may
 * always get a canonical form using {@link #canonicalForm()}.
 */
@JsonSerialize
@JsonDeserialize
@TextGroupImmutable
@Value.Immutable
public abstract class Names implements Iterable<Name>, PotentiallyAbsentSerifTheory,
  WithNames {

  @Value.Default
  @Override
  public boolean isAbsent() {
    return false;
  }

  public abstract TokenSequence tokenSequence();
  public abstract ImmutableSet<Name> names();
  public abstract Optional<Double> score();

  protected void check() {
    checkArgument(!isAbsent() || names().isEmpty());
    checkArgument(!isAbsent() || !score().isPresent());
  }

  public static Names absent(TokenSequence ts) {
    return new Names.Builder()
        .tokenSequence(ts)
        .isAbsent(true).build();
  }


  public final Name name(int idx) {
    return names().asList().get(idx);
  }

  public int numNames() {
    return names().size();
  }

  public int size() {
    return numNames();
  }

  public Name get(int idx) {
    return name(idx);
  }

  @Override
  public final Iterator<Name> iterator() {
    return names().iterator();
  }

  public final ImmutableList<Name> asList() {
    return names().asList();
  }

  private static final Function<Iterable<Name>, Name> MAX_BY_SCORE =
      maxFunction(
          Ordering.natural().onResultOf(
              compose(
                  deoptionalizeFunction(Double.NEGATIVE_INFINITY),
                  NameFunctions.score())));

  /**
   * Returns a copy of this {@link Names} in canonical form (see class Javadoc).
   */
  @Value.Lazy
  public Names canonicalForm() {
    final ImmutableListMultimap<Name, Name> scoreNeutralized =
        Multimaps.index(names(), new Function<Name, Name>() {
          @Override
          public Name apply(final Name x) {
            return x.withScore(Optional.<Double>absent());
          }
        });
    if (scoreNeutralized.keySet().size() != scoreNeutralized.size()) {
      // for each group of names differing only in score, keep only the one with the maximum scores
      return this.withNames(MultimapUtils.reduceToMap(scoreNeutralized, MAX_BY_SCORE).values());
    } else {
      // already in canonical form
      return this;
    }
  }

  /**
   * Gets the offsets of all tokens which appear in names
   */
  public final ImmutableRangeSet<TokenOffset> tokensCoveredByNames() {
    final TreeRangeSet<TokenOffset> ret = TreeRangeSet.create();
    for (final Name name : names()) {
      ret.add(Range.closed(
          TokenOffset.asTokenOffset(name.span().startTokenIndexInclusive()),
          TokenOffset.asTokenOffset(name.span().endTokenIndexInclusive())));
    }
    return ImmutableRangeSet.copyOf(ret);
  }

  public static Names createFrom(Iterable<Name> nameSpans, TokenSequence tokenSequence, Double score) {
    return new Builder().names(nameSpans).tokenSequence(tokenSequence).score(score).build();
  }

  public static Names createEmpty(TokenSequence tokenSequence, double score) {
    return new Builder().tokenSequence(tokenSequence).score(score).build();
  }

  public static class Builder extends ImmutableNames.Builder {}
}
