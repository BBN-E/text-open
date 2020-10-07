package com.bbn.serif.theories;

import com.bbn.bue.common.UnicodeFriendlyString;

import com.google.common.base.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for creating and working with {@link com.bbn.serif.theories.TokenSpan}s.
 *
 * This class provides the prefered way of creating a cross-sentence token span:
 * {@code TokenSpans.from(startTok).through(endTok).}.
 *
 */
public final class TokenSpans {

  private TokenSpans() {
    throw new UnsupportedOperationException();
  }

  /**
   *@deprecated Prefer {@link #from(Token)}.
   */
  @Deprecated
  public static Builder fromInclusiveIndex(TokenSequence startTokenSequence,
      int startIdxInclusive) {
    return new Builder(startTokenSequence.token(startIdxInclusive));
  }

  /**
   * Createa a token span.  Use as follows: {@code TokenSpans.from(startTok).through(endTok).}
   */
  public static Builder from(Token startTokInclusive) {
    return new Builder(startTokInclusive);
  }

  public static Function<TokenSequence.Span, UnicodeFriendlyString> tokenizedTextFunction() {
    return TokenizedTextFunction.INSTANCE;
  }

  public static class Builder {
    private final Token startTokInclusive;

    private Builder(Token startTokInclusive) {
      this.startTokInclusive = checkNotNull(startTokInclusive);
    }

    /**
     * @deprecated Prefer {@link #through(Token)}.
     */
    public TokenSpan toInclusiveIndex(TokenSequence endTokenSequence, int endIdxInclusive) {
      return through(endTokenSequence.token(endIdxInclusive));
    }

    @SuppressWarnings("ReferenceEquality")
    public TokenSpan through(Token endTokInclusive) {
      if (startTokInclusive.tokenSequence() == endTokInclusive.tokenSequence()) {
        return startTokInclusive.tokenSequence().span(startTokInclusive.index(),
            endTokInclusive.index());
      } else {
        return new DefaultTokenSpan(startTokInclusive, endTokInclusive);
      }
    }
  }

  private static class DefaultTokenSpan extends AbstractTokenSpan implements TokenSpan {
    private final Token startInclusive;
    private final Token endInclusive;

    public DefaultTokenSpan(Token startInclusive, Token endInclusive) {
      this.startInclusive = checkNotNull(startInclusive);
      this.endInclusive = checkNotNull(endInclusive);
      checkArgument(endInclusive.tokenSequence().sentenceIndex()
              >= startInclusive.tokenSequence().sentenceIndex(),
          "Sentence number for end of token span ({}) precedes that for start ({}): ",
          endInclusive.tokenSequence().sentenceIndex(),
          startInclusive.tokenSequence().sentenceIndex());
    }

    @Override
    public TokenSequence startTokenSequence() {
      return startInclusive.tokenSequence();
    }

    @Override
    public TokenSequence endTokenSequence() {
      return endInclusive.tokenSequence();
    }

    @Override
    public int startTokenIndexInclusive() {
      return startInclusive.index();
    }

    @Override
    public int endTokenIndexInclusive() {
      return endInclusive.index();
    }
  }

  private enum TokenizedTextFunction
      implements Function<TokenSequence.Span, UnicodeFriendlyString> {
    INSTANCE;

    @Override
    public UnicodeFriendlyString apply(final TokenSequence.Span input) {
      return input.tokenizedText();
    }
  }
}
