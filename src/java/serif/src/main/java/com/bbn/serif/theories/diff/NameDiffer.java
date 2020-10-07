package com.bbn.serif.theories.diff;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Name;

import com.google.common.base.Optional;

import javax.inject.Inject;

/**
 * Determines the differences, if any, between two {@link Name}s
 */
final class NameDiffer implements Differ<Name, NameDiff> {
  private final boolean ignoreScores;

  @Inject
  NameDiffer(@IgnoreScoresWhenDiffingP final boolean ignoreScores) {
    this.ignoreScores = ignoreScores;
  }

  @Override
  public Optional<NameDiff> diff(final Name left, final Name right) {
    final Optional<Difference<Symbol>>
        typeDiff = AtomicDiff.diffUsingEquality(left.type().name(), right.type().name());
    final Optional<Difference<Double>> scoreDiff =
        ignoreScores ? Optional.<Difference<Double>>absent()
                     : AtomicDiff.diffUsingEquality(left.score(), right.score());

    if (typeDiff.isPresent() || scoreDiff.isPresent()) {
      return Optional.of(new NameDiff.Builder().left(left).right(right).typeDiff(typeDiff).scoreDiff(scoreDiff).build());
    } else {
      return Optional.absent();
    }
  }
}
