package com.bbn.serif.theories;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.EDTOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.strings.offsets.OffsetRange;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/* package-private*/ abstract class AbstractTokenSpan implements TokenSpan {

  @Override
  public final boolean inSingleSentence() {
    return startTokenSequence().equals(endTokenSequence());
  }

  @Override
  public final int startSentenceIndex() {
    return startTokenSequence().sentenceIndex();
  }

  @Override
  public final int endSentenceIndex() {
    return endTokenSequence().sentenceIndex();
  }

  @Override
  public final boolean overlapsSentenceIndex(int sentIndex) {
    return startSentenceIndex() <= sentIndex && sentIndex <= endSentenceIndex();
  }

  @Override
  public final boolean isSingleToken() {
    return inSingleSentence() && startTokenIndexInclusive() == endTokenIndexInclusive();
  }

  @Override
  public final CharOffset startCharOffset() {
    return startToken().startCharOffset();
  }

  @Override
  public final OffsetGroup startOffsetGroup() {
    return startToken().startOffsetGroup();
  }

  @Override
  public final OffsetGroup endOffsetGroup() {
    return endToken().endOffsetGroup();
  }

  @Override
  public final CharOffset endCharOffset() {
    return endToken().endCharOffset();
  }

  @Override
  public final EDTOffset startEDTOffset() {
    return startToken().startEDTOffset();
  }

  @Override
  public final EDTOffset endEDTOffset() {
    return endToken().endEDTOffset();
  }

  @Override
  public final Token startToken() {
    return startTokenSequence().token(start());
  }

  @Override
  public final Token endToken() {
    return endTokenSequence().token(end());
  }

  @Override
  public final boolean contains(final TokenSpan other) {
    checkNotNull(other);
    return startTokenSequence().sentenceIndex() <= other.startTokenSequence().sentenceIndex()
        && startTokenIndexInclusive() <= other.startTokenIndexInclusive()
        && endTokenSequence().sentenceIndex() >= other.endTokenSequence().sentenceIndex()
        && endTokenIndexInclusive() >= other.endTokenIndexInclusive();
  }

  @Override
  public final boolean overlaps(final TokenSpan other) {
    checkNotNull(other);

    final boolean startsInOther = other.startToken().precedesOrIs(startToken())
        && startToken().precedesOrIs(other.endToken());
    final boolean endsInOther = other.startToken().precedesOrIs(endToken())
        && endToken().precedesOrIs(other.endToken());
    return (startsInOther || endsInOther || contains(other) || other.contains(this));
  }

  /**
   * @return whether this span starts strictly before <tt>other</tt>.
   */
  @Override
  public final boolean startsBefore(final TokenSpan other) {
    checkNotNull(other);
    return startTokenSequence().sentenceIndex() < other.startTokenSequence().sentenceIndex()
        || startTokenIndexInclusive() < other.startTokenIndexInclusive();
  }

  /**
   * @return whether this span ends strictly before <tt>other</tt> begins
   */
  @Override
  public final boolean endsBefore(final TokenSpan other) {
    checkNotNull(other);
    return endTokenSequence().sentenceIndex() < other.endTokenSequence().sentenceIndex()
        || endTokenIndexInclusive() < other.startTokenIndexInclusive();
  }

  /**
   * @return Whether this span starts strictly after <tt>other</tt>.
   */
  @Override
  public final boolean startsAfter(final TokenSpan other) {
    checkNotNull(other);
    return startTokenSequence().sentenceIndex() > other.startTokenSequence().sentenceIndex()
        || startTokenIndexInclusive() > other.startTokenIndexInclusive();
  }

  /**
   * @return Whether this {@linke TokenSpan} ends stricly after <tt>other</tt>/
   */
  @Override
  public final boolean endsAfter(final TokenSpan other) {
    checkNotNull(other);
    return endTokenSequence().sentenceIndex() > other.endTokenSequence().sentenceIndex()
        || endTokenIndexInclusive() > other.endTokenIndexInclusive();
  }

  // suppressed because only spans from the exact same token sequence can be compred
  @SuppressWarnings("ReferenceEquality")
  @Override
  public boolean containedInSameSentenceAs(TokenSpan other) {
    return inSingleSentence() && other.inSingleSentence()
        && startTokenSequence() == other.startTokenSequence();
  }

  @Override
  public final OffsetRange<CharOffset> charOffsetRange() {
    return OffsetRange.fromInclusiveEndpoints(startCharOffset(), endCharOffset());
  }

  @Override
  public final OffsetGroupRange offsetRange() {
    return OffsetGroupRange.from(startOffsetGroup(), endOffsetGroup());
  }

  /**
   * Gets the unaltered string of the original document text covered by this {@link com.bbn.serif.theories.TokenSpan}.
   * If the original text of the document is not available, returns {@link com.google.common.base.Optional#absent()}.
   */
  @Override
  public final UnicodeFriendlyString rawOriginalText() {
    final LocatedString originalText = startTokenSequence().documentOriginalText();
    // original text is guaranteed to have a reference string
    //noinspection OptionalGetWithoutIsPresent
    return originalText.referenceSubstringByContentOffsets(OffsetRange.fromInclusiveEndpoints(
        startToken().originalTextContentRange().startInclusive(),
        endToken().originalTextContentRange().endInclusive())).get();
  }

  /**
   * Gets the {@link com.bbn.bue.common.strings.LocatedString} of the original document text covered
   * by this {@link com.bbn.serif.theories.TokenSpan}. If the original text of the document is not
   * available, returns {@link com.google.common.base.Optional#absent()}. Unless you need the
   * additional offset information a {@link com.bbn.bue.common.strings.LocatedString} provides,
   * prefer {@link #rawOriginalText()}.
   */
  @Override
  public final LocatedString originalText() {
    return startTokenSequence().documentOriginalText().contentLocatedSubstringByContentOffsets(
        OffsetRange.fromInclusiveEndpoints(
            startToken().originalTextContentRange().startInclusive(),
            endToken().originalTextContentRange().endInclusive()));
  }

  @Override
  public final List<Token> tokens(DocTheory dt) {
    final List<Token> tokens = Lists.newArrayList();
    if (inSingleSentence()) {
      for (int i = startTokenIndexInclusive(); i <= endTokenIndexInclusive(); ++i) {
        tokens.add(startTokenSequence().token(i));
      }
    } else {
      // in starting sentence, from start token to end of sentence
      for (int i = startTokenIndexInclusive(); i < startTokenSequence().size(); ++i) {
        tokens.add(startTokenSequence().token(i));
      }

      // text of intervening sentences
      for (int interveningSentenceIdx = startTokenSequence().sentenceIndex() + 1;
           interveningSentenceIdx < endTokenSequence().sentenceIndex(); ++interveningSentenceIdx) {
        tokens
            .addAll(dt.sentenceTheory(interveningSentenceIdx).tokenSequence().span().tokens(dt));
      }

      // in ending sentence, from start of sentence to end token
      for (int i = 0; i <= endTokenIndexInclusive(); ++i) {
        tokens.add(endTokenSequence().token(i));
      }
    }
    return tokens;
  }

  @Override
  public final int numTokens(DocTheory dt) { return tokens(dt).size(); }

  @Override
  public UnicodeFriendlyString tokenizedText(DocTheory dt) {
    List<Token> tokens = tokens(dt);
    List<String> tokenStrings = Lists.newArrayList();
    for (Token token : tokens) {
      tokenStrings.add(token.tokenizedText().utf16CodeUnits());
    }
    // concatenation is safe even if mixing BMP and non-BMP
    return StringUtils.unicodeFriendly(StringUtils.spaceJoiner().join(tokenStrings));
  }

  @Override
  public final int hashCode() {
    return Objects.hashCode(startTokenSequence(), endTokenSequence(),
        startTokenIndexInclusive(), endTokenIndexInclusive());
  }

  @Override
  public final boolean equals(final Object o) {
    if (o == null) {
      return false;
    }

    if (!(o instanceof TokenSpan)) {
      return false;
    }

    final TokenSpan otherSpan = (TokenSpan) o;

    return Objects.equal(startTokenIndexInclusive(), otherSpan.startTokenIndexInclusive())
        && Objects.equal(endTokenIndexInclusive(), otherSpan.endTokenIndexInclusive())
        && Objects.equal(startTokenSequence(), otherSpan.startTokenSequence())
        && Objects.equal(endTokenSequence(), otherSpan.endTokenSequence());
  }

  // convenience methods for shorter typing
  private int start() {
    return startTokenIndexInclusive();
  }

  private int end() {
    return endTokenIndexInclusive();
  }

  // deprecated methods
  @Deprecated
  @Override
  public final Optional<UnicodeFriendlyString> originalText(DocTheory dt) {
    return Optional.of(dt.document().originalText().content().substringByCodePoints(
        OffsetRange.fromInclusiveEndpoints(
            startToken().originalTextContentRange().startInclusive(),
            endToken().originalTextContentRange().endInclusive())));
  }

}
