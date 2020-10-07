package com.bbn.serif.tokens;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.Document;
import com.bbn.serif.theories.Sentence;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.tokens.constraints.MustBeAToken;
import com.bbn.serif.tokens.constraints.MustBeTokenized;
import com.bbn.serif.tokens.constraints.NoTokenMayContain;
import com.bbn.serif.tokens.constraints.TokenBoundaryProposer;
import com.bbn.serif.tokens.constraints.TokenizationConstraint;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkState;

/**
 * Provides a Tokenization subject to a set of {@code TokenConstraints} which specify whether or not
 * something must be a token, or must not be one, or must not be further split. Allows proposing of
 * additional boundaries via {@code TokenBoundaryProposers}
 */
@Beta
public class SimpleTokenizer extends AbstractSentenceTokenizer implements TokenFinder {

  private static final Logger log = LoggerFactory.getLogger(SimpleTokenizer.class);

  private final boolean tolerant;

  private final ImmutableSet<TokenBoundaryProposer> boundaryProposers;

  // TODO multibinder for these boundary proposers
  //  SimpleTokenizer(final Set<TokenBoundaryProposer> boundaryProposers, final boolean tolerant) {
  @Inject
  SimpleTokenizer(@TolerantP final boolean tolerant) {
    this.tolerant = tolerant;
    this.boundaryProposers = ImmutableSet.of();
  }

  @Override
  public SentenceTheory tokenize(final Document document, final Sentence sentence,
      final Set<TokenizationConstraint> constraints) {
    final LocatedString sentenceText = sentence.locatedString();
    // TODO: make this efficient
    final OffsetList<TokenizationConstraintTypes> constraintList =
        new OffsetList<>(sentenceText.referenceBounds().startCharOffsetInclusive().asInt(),
            sentenceText.referenceBounds().endEdtOffsetInclusive().asInt());

    for (final TokenizationConstraint constraint : constraints) {
      if (constraint instanceof MustBeAToken) {
        final OffsetGroupRange offsetGroupRange = ((MustBeAToken) constraint).offsets();
        int start = offsetGroupRange.startInclusive().charOffset().asInt();
        int end = offsetGroupRange.endInclusive().charOffset().asInt();
        for (int i = start; i <= end; i++) {
          constraintList.setMustNotHaveBeenSet(i, TokenizationConstraintTypes.UNSPLITTABLE_TOKEN);
        }
        constraintList.set(start, TokenizationConstraintTypes.UNSPLITTABLE_TOKEN_START);
        constraintList.set(end, TokenizationConstraintTypes.UNSPLITTABLE_TOKEN_END);
        if (start == end) {
          constraintList.set(start, TokenizationConstraintTypes.SINGLETON_TOKEN);
        }
      } else if (constraint instanceof MustBeTokenized) {
        final OffsetGroupRange offsetGroupRange = ((MustBeTokenized) constraint).offsets();
        int start = offsetGroupRange.startInclusive().charOffset().asInt();
        int end = offsetGroupRange.endInclusive().charOffset().asInt();
        // everything in this type of token may be split.
        for (int i = start; i <= end; i++) {
          checkState(constraintList.get(i) == null || !constraintList.get(i).isUnsplittable());
          // only mark as splittable if this isn't already marked.
          if (constraintList.get(i) == null) {
            constraintList.set(i, TokenizationConstraintTypes.SPLITTABLE_TOKEN);
          }
        }

        // let's not overwrite singletons. This also handles new singleton tokens (often appear as annotation errors?)
        if (constraintList.get(start) == null
            || constraintList.get(start).equals(TokenizationConstraintTypes.SPLITTABLE_TOKEN)
            || constraintList.get(start).equals(TokenizationConstraintTypes.START_TOKEN)) {
          constraintList.set(start, TokenizationConstraintTypes.START_TOKEN);
        } else if (constraintList.get(start).isEnd() || constraintList.get(start).isSingleton()) {
          constraintList.set(start, TokenizationConstraintTypes.SINGLETON_TOKEN);
        } else {
          throw new RuntimeException(
              "Attempted to overwrite a tokenization constraint of " + constraintList.get(start)
                  + " with " + TokenizationConstraintTypes.START_TOKEN + " at " + start);
        }
        if (constraintList.get(end) == null
            || constraintList.get(end).equals(TokenizationConstraintTypes.SPLITTABLE_TOKEN)
            || constraintList.get(end).equals(TokenizationConstraintTypes.END_TOKEN)) {
          constraintList.set(end, TokenizationConstraintTypes.END_TOKEN);
        } else if (constraintList.get(end).isStart() || constraintList.get(end).isSingleton()) {
          constraintList.set(end, TokenizationConstraintTypes.SINGLETON_TOKEN);
        } else {
          throw new RuntimeException(
              "Attempted to overwrite a tokenization contraint of " + constraintList.get(end)
                  + " with " + TokenizationConstraintTypes.END_TOKEN + " at " + end);
        }
        if (start == end) {
          constraintList.set(start, TokenizationConstraintTypes.SINGLETON_TOKEN);
        }
      } else if (constraint instanceof NoTokenMayContain) {
        final OffsetGroup offsetGroup = ((NoTokenMayContain) constraint).offsets();
        constraintList
            .setMustNotHaveBeenSet(offsetGroup.charOffset().asInt(),
                TokenizationConstraintTypes.SPLIT_POINT);
      } else {
        throw new RuntimeException("Unexpected TokenizationConstraint " + constraint.getClass());
      }
    }

    // add the contents of the proposers if at all possible
    // TODO: should these shift to directly using the enum of constraint types?
    for (final TokenBoundaryProposer proposer : boundaryProposers) {
      addConstraintsIfFeasible(constraintList, proposer.offsetsForText(sentenceText));
    }
    // TODO put this in a TokenBoundaryPropser
    for (int i = constraintList.start; i <= constraintList.end; i++) {
      if ((constraintList.get(i) == null
               || constraintList.get(i).equals(TokenizationConstraintTypes.SPLITTABLE_TOKEN))
          && sentenceText.content().substringByCodePoints(OffsetRange.charOffsetRange(i, i + 1))
          .trim().isEmpty()) {
        constraintList.set(i, TokenizationConstraintTypes.SPLIT_POINT);
      }
    }


    // set everything else to unknown
    for (int i = constraintList.start; i <= constraintList.end; i++) {
      if (constraintList.get(i) == null) {
        constraintList.set(i, TokenizationConstraintTypes.UNKNOWN);
      }
    }

    final TokenSequence.FromTokenDataBuilder builder =
        TokenSequence.withOriginalText(sentence.sentenceNumber(), sentenceText);

    // invariant: we never split an "unsplittable region" <- above we cannot set a token start/end inside an unsplittable region
    // invariant: no split points ever appear inside a token

    // invariant: we never have an end index before a start index
    // base case: it can't happen above
    // inductive step: that end index is forced into a token below

    for (int i = skipSplitPoints(constraintList, constraintList.start); i < constraintList.end; ) {
      // skips anything that's not a start token
      final Range<Integer> nextTokenRange = extractTokenRangeFrom(constraintList, i);
      final int start = nextTokenRange.lowerEndpoint();
      final int end = nextTokenRange.upperEndpoint();
      checkState(end >= 0);
      checkState(start <= end);
      checkState(validToken(constraintList, start, end), "Constructed an invalid token!");
      final UnicodeFriendlyString rawText =
          sentenceText.content().substringByCodePoints(CharOffset.asCharOffset(start),
          CharOffset.asCharOffset(end));
      final UnicodeFriendlyString text;
      if(rawText.trim().isEmpty()) {
        text = StringUtils.unicodeFriendly("-WHITESPACE-");
      } else {
        text = rawText;
      }
      builder.addToken(Symbol.from(text.utf16CodeUnits()),
          OffsetRange.charOffsetRange(start, end) );
      i = skipSplitPoints(constraintList, end + 1);
    }

    final TokenSequence tokenSequence = builder.build();
    // verify this is correct.
    for (final TokenizationConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(sentenceText, tokenSequence)) {
        if (tolerant) {
          log.warn("Invalid tokens for constraint " + constraint);
        } else {
          throw new SerifException(
              "Invalid tokens for constraint " + constraint + " and tokenization " + tokenSequence);
        }
      }
    }

    log.info("constraintsList " + constraintList);

    return SentenceTheory.createForTokenSequence(sentence, tokenSequence).build();
  }

  private int skipSplitPoints(
      final OffsetList<TokenizationConstraintTypes> constraintList, int i) {
    while (i < constraintList.end && constraintList.get(i).isSplitPoint()) {
      i++;
    }
    return Math.min(i, constraintList.end);
  }

  private Range<Integer> extractTokenRangeFrom(
      final OffsetList<TokenizationConstraintTypes> constraintList, final int start) {

    if (constraintList.get(start).equals(TokenizationConstraintTypes.SINGLETON_TOKEN)
        || constraintList.get(start).equals(TokenizationConstraintTypes.END_TOKEN)) {
      return Range.closed(start, start);
    }
    int end = start + 1;

    while (end <= constraintList.end) {
      if (constraintList.get(end).equals(TokenizationConstraintTypes.SINGLETON_TOKEN)
          || constraintList.get(end).isSplitPoint()
          || constraintList.get(end).isStart()) {
        end--;
        break;
      }
      if (constraintList.get(end).isEnd()) {
        break;
      }
      end++;
    }
    return Range.closed(start, Math.min(end, constraintList.end));
  }

  private boolean validToken(final OffsetList<TokenizationConstraintTypes> constraintList,
      final int start,
      final int end) {
    if (constraintList.get(start).equals(TokenizationConstraintTypes.UNSPLITTABLE_TOKEN)
        || constraintList.get(end)
        .equals(TokenizationConstraintTypes.UNSPLITTABLE_TOKEN)) {
      return false;
    }
    for (int i = start; i <= end; i++) {
      if (constraintList.get(i).isSplitPoint()) {
        return false;
      }
    }
    return true;
  }

  private void addConstraintsIfFeasible(
      final OffsetList<TokenizationConstraintTypes> constraintList,
      final ImmutableSet<OffsetGroupRange> offsetGroupRanges) {
    for (final OffsetGroupRange offsetGroupRange : offsetGroupRanges) {
      final int start = offsetGroupRange.startInclusive().charOffset().asInt();
      final int end = offsetGroupRange.endInclusive().charOffset().asInt();
      if (constraintList.get(start) == null) {
        constraintList.set(start, TokenizationConstraintTypes.START_TOKEN);
      }
      if (constraintList.get(end) == null) {
        constraintList.set(end, TokenizationConstraintTypes.END_TOKEN);
      }
    }
  }

  @Override
  public void finish() throws IOException {

  }

  private enum TokenizationConstraintTypes {
    UNKNOWN,

    // unsplittables
    UNSPLITTABLE_TOKEN,
    UNSPLITTABLE_TOKEN_START,
    UNSPLITTABLE_TOKEN_END,

    // for one character length tokens
    SINGLETON_TOKEN,

    SPLITTABLE_TOKEN,
    START_TOKEN,
    END_TOKEN,

    SPLIT_POINT; // also NOT_IN_A_TOKEN

    boolean isUnsplittable() {
      return this.equals(UNSPLITTABLE_TOKEN) || this.equals(UNSPLITTABLE_TOKEN_START)
          || this.equals(UNSPLITTABLE_TOKEN_END) || this.equals(SPLIT_POINT);
    }

    boolean isStart() {
      return this.equals(START_TOKEN) || this.equals(UNSPLITTABLE_TOKEN_START);
    }

    boolean isEnd() {
      return this.equals(END_TOKEN) || this.equals(UNSPLITTABLE_TOKEN_END);
    }

    boolean isSplitPoint() {
      return this.equals(SPLIT_POINT);
    }

    public boolean isSingleton() {
      return this.equals(SINGLETON_TOKEN);
    }
  }

  private final static class OffsetList<T> {

    final T[] parts;
    final int start;
    final int end;

    // inclusive
    @SuppressWarnings("Unchecked")
    private OffsetList(int start, int end) {
      this.start = start;
      this.parts = (T[]) new Object[end - start + 1];
      this.end = end;
    }

    public void set(int ind, T val) {
      parts[ind - start] = val;
    }

    public T get(int ind) {
      return parts[ind - start];
    }

    public void setMustNotHaveBeenSet(int ind, T val) {
      T old = get(ind);
      if (old == null) {
        set(ind, val);
      } else {
        throw new IllegalStateException(
            "Cannot set value twice at " + ind + " with value " + old + " and " + val);
      }
    }
  }


}
