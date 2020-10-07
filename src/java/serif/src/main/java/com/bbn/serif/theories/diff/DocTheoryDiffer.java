package com.bbn.serif.theories.diff;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Diffs two doc theories. Currently only compares the sentence theories, aligned by sentence number.
 */
@Beta
final class DocTheoryDiffer implements Differ<DocTheory, DocTheoryDiff> {
  private final SentenceTheoryDiffer sentenceTheoryDiffer;

  @Inject
  DocTheoryDiffer(final SentenceTheoryDiffer sentenceTheoryDiffer) {
    this.sentenceTheoryDiffer = checkNotNull(sentenceTheoryDiffer);
  }

  @Override
  public Optional<DocTheoryDiff> diff(final DocTheory left, final DocTheory right) {
    final ImmutableList.Builder<SentenceTheory> leftOnlyB = ImmutableList.builder();
    final ImmutableList.Builder<SentenceTheory> rightOnlyB = ImmutableList.builder();
    final ImmutableList.Builder<SentenceTheoryDiff> diffsB = ImmutableList.builder();

    for (int i=0; i<Math.min(left.numSentences(), right.numSentences()); ++i) {
      final SentenceTheory leftSentence = left.sentenceTheory(i);
      final SentenceTheory rightSentence = right.sentenceTheory(i);

      final Optional<SentenceTheoryDiff> sentenceDiff =
          sentenceTheoryDiffer.diff(leftSentence, rightSentence);

      if (sentenceDiff.isPresent()) {
        diffsB.add(sentenceDiff.get());
      }
    }

    for (int i=left.numSentences(); i<right.numSentences(); ++i) {
      rightOnlyB.add(right.sentenceTheory(i));
    }

    for (int i=right.numSentences(); i<left.numSentences(); ++i) {
      leftOnlyB.add(left.sentenceTheory(i));
    }

    final ImmutableList<SentenceTheory> leftOnly = leftOnlyB.build();
    final ImmutableList<SentenceTheory> rightOnly = rightOnlyB.build();
    final ImmutableList<SentenceTheoryDiff> diffs = diffsB.build();

    if (leftOnly.isEmpty() && rightOnly.isEmpty() && diffs.isEmpty()) {
      return Optional.absent();
    } else {
      // we really want to include empty sentence theories too
      @SuppressWarnings("deprecation")
      final CollectionDifference<List<SentenceTheory>, SentenceTheory, SentenceTheoryDiff> sentenceDiffs =
          CollectionDifference.from((List<SentenceTheory>) left.sentenceTheories(),
              (List<SentenceTheory>) right.sentenceTheories(),
              leftOnly, rightOnly, diffs);
      return Optional.of(new DocTheoryDiff.Builder().left(left).right(right).sentenceTheoryDiffs(sentenceDiffs).build());
    }
  }
}
