package com.bbn.serif.theories;

import com.bbn.bue.common.LetterCounts;
import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Predicate;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Detects whether a sentence appears to be "monocase" - that is, all upper or all lower case.
 *
 * This will be injectable when https://github.com/immutables/immutables/issues/317 is released.
 */
@JsonSerialize
@JsonDeserialize
@TextGroupImmutable
@Value.Immutable
public abstract class MonocaseDetector implements Predicate<SentenceTheory> {

  private static final TokenSequenceCaseDetector caseDetector = TokenSequences.caseDetector();

  public abstract int minLength();

  public abstract double minCasableRatio();

  public abstract double minLetterRatio();

  @Value.Check
  protected void check() {
    checkArgument(minLength() >= 0);
    checkArgument(minCasableRatio() >= 0.0 && minCasableRatio() <= 1.0);
    checkArgument(minLetterRatio() >= 0.0 && minLetterRatio() <= 1.0);
  }

  @Override
  public boolean apply(final SentenceTheory x) {
    if (x.tokenSequence().size() >= minLength()) {
      final LetterCounts allCharsCasing =
          caseDetector.detectCasing(x.tokenSequence()).forAllCharacters();

      if (allCharsCasing.numCodepoints() > 0 && allCharsCasing.numLetters() > 0) {
        // are all letters capable of case differences monocase?
        final boolean isMonocaseWhereApplicable = allCharsCasing.isAllLowercaseWhereApplicable()
            || allCharsCasing.isAllUppercaseWhereApplicable();
        // fraction of code points which are letters
        final double letterRatio = allCharsCasing.numLetters()
            / ((double) allCharsCasing.numCodepoints());
        // fraction of letter code points which are capable of case differences
        final double casableRatio = 1.0 - allCharsCasing.numberOfUncasedLetters()
            / ((double) allCharsCasing.numLetters());

        return isMonocaseWhereApplicable
            && letterRatio >= minLetterRatio()
            && casableRatio > minCasableRatio();
      }
    }

    return false;
  }

  public static class Builder extends ImmutableMonocaseDetector.Builder {}
}
