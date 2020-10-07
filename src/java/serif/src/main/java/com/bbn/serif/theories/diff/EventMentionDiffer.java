package com.bbn.serif.theories.diff;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.types.Genericity;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Polarity;
import com.bbn.serif.types.Tense;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.concat;

/**
 * Determines whether or not two event mentions differ and, if so, how.
 *
 * It is configurable whether or not to pay attention to scores.
 */
@Beta
final class EventMentionDiffer implements Differ<EventMention, EventMentionDiff> {

  final boolean ignoreScores;
  final SynNodeDiffer synNodeDiffer;

  @Inject
  EventMentionDiffer(@IgnoreScoresWhenDiffingP final boolean ignoreScores,
      final SynNodeDiffer synNodeDiffer) {
    this.ignoreScores = ignoreScores;
    this.synNodeDiffer = checkNotNull(synNodeDiffer);
  }

  @Override
  public Optional<EventMentionDiff> diff(final EventMention left,
      final EventMention right) {
    final Optional<Difference<Symbol>> eventTypeDiff =
        AtomicDiff.diffUsingEquality(left.type(), right.type());

    // skip diffing pattern IDs for now

    final Optional<Difference<Modality>> modalityDiff =
        AtomicDiff.diffUsingEquality(left.modality(), right.modality());

    final Optional<Difference<Polarity>> polarityDiff =
        AtomicDiff.diffUsingEquality(left.polarity(), right.polarity());

    final Optional<Difference<Genericity>> genericityDiff =
        AtomicDiff.diffUsingEquality(left.genericity(), right.genericity());

    final Optional<Difference<Tense>> tenseDiff =
        AtomicDiff.diffUsingEquality(left.tense(), right.tense());

    // don't diff indicators and gain loss for now
    final Optional<Difference<Double>> modalityScoreDiff =
        ignoreScores ? Optional.<Difference<Double>>absent()
                     : AtomicDiff.diffUsingEquality(left.modalityScore(), right.modalityScore());

    final Optional<Difference<Double>> genericityScoreDiff =
        ignoreScores ? Optional.<Difference<Double>>absent()
                     : AtomicDiff.diffUsingEquality(left.genericityScore(), right.genericityScore());

    final Optional<Difference<Double>> scoreDiff =
        ignoreScores ? Optional.<Difference<Double>>absent()
                     : AtomicDiff.diffUsingEquality(left.score(), right.score());

    // we currently ignore the anchor proposition!

    final Optional<Difference<SynNode>> anchorNodeDiff =
        synNodeDiffer.diff(left.anchorNode(), right.anchorNode());

    // inefficient n^2 implementation, currently only checks for exact match and doesn't
    // build more detailed diffs
    //final List<EventMention.Argument> rightOnlyArgs = Lists.newArrayList();

    final List<EventMention.Argument> leftOnlyArgs = computeOneSideOnlyArgs(left, right);
    final List<EventMention.Argument> rightOnlyArgs = computeOneSideOnlyArgs(right, left);

    final Optional<CollectionDifference<List<EventMention.Argument>, EventMention.Argument, Difference<EventMention.Argument>>>
        argDiff = (leftOnlyArgs.isEmpty() && rightOnlyArgs.isEmpty())
                  ?Optional.<CollectionDifference<List<EventMention.Argument>,EventMention.Argument,Difference<EventMention.Argument>>>absent()
    :Optional.of(CollectionDifference.from(left.arguments(), right.arguments(), leftOnlyArgs,
        rightOnlyArgs, ImmutableList.<Difference<EventMention.Argument>>of()));

    if (Iterables
        .isEmpty(concat(eventTypeDiff.asSet(), modalityDiff.asSet(),
            polarityDiff.asSet(), genericityDiff.asSet(), tenseDiff.asSet(),
            modalityScoreDiff.asSet(), genericityScoreDiff.asSet(),
            scoreDiff.asSet(), anchorNodeDiff.asSet(), argDiff.asSet()))) {
      return Optional.absent();
    } else {
      return Optional
          .of(new EventMentionDiff.Builder().left(left).right(right).eventTypeDiff(eventTypeDiff).argumentDiffs(argDiff)
              .modalityDiff(modalityDiff).modalityScoreDiff(modalityScoreDiff).polarityDiff(polarityDiff)
              .genericityDiff(genericityDiff).genericityScoreDiff(genericityScoreDiff)
              .tenseDiff(tenseDiff).anchorNodeDiff(anchorNodeDiff).scoreDiff(scoreDiff).build());
    }
  }

  @Nonnull
  private List<EventMention.Argument> computeOneSideOnlyArgs(final EventMention left,
      final EventMention right) {
    final List<EventMention.Argument> leftOnlyArgs = Lists.newArrayList();

    for (final EventMention.Argument leftArg : left.arguments()) {
      boolean foundMatch = false;
      for (final EventMention.Argument rightArg : right.arguments()) {
        foundMatch = foundMatch || !argDiff(leftArg, rightArg);
      }
      if (!foundMatch) {
        leftOnlyArgs.add(leftArg);
      }
    }
    return leftOnlyArgs;
  }

  private boolean argDiff(final EventMention.Argument leftArg,
      final EventMention.Argument rightArg) {
    if (leftArg.getClass() == rightArg.getClass()
        && leftArg.role() == rightArg.role()
        && (ignoreScores || leftArg.score() == rightArg.score())) {
      if (leftArg instanceof EventMention.MentionArgument) {
        final EventMention.MentionArgument leftMentArg
            = (EventMention.MentionArgument) leftArg;
        final EventMention.MentionArgument rightMentArg
            = (EventMention.MentionArgument) rightArg;
        return !leftMentArg.mention().span().tokenizedText().equals(
            rightMentArg.mention().span().tokenizedText());
      } else if (leftArg instanceof EventMention.ValueMentionArgument) {
        final EventMention.ValueMentionArgument leftVMArg =
            (EventMention.ValueMentionArgument) leftArg;
        final EventMention.ValueMentionArgument rightVMArg =
            (EventMention.ValueMentionArgument) rightArg;
        return !leftVMArg.valueMention().span().tokenizedText().equals(
            rightVMArg.valueMention().span().tokenizedText());
      }
    }
    return true;
  }


}


