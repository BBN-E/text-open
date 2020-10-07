package com.bbn.serif.theories;

import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.ASRTime;
import com.bbn.bue.common.strings.offsets.ByteOffset;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.EDTOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.strings.offsets.TokenOffset;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A token in a document. This is the basic unit of Serif processing. The token string need not have
 * any particular relationship to the original documen text.
 *
 * Two {@code Token}s are equal if they have the same {@link #symbol()}, the same offsets, and
 * occupy the same sentence index in their owning {@link TokenSequence}s.
 */
public final class Token implements HasToken, Spanning {
  private TokenSequence tokenSequence = null;
  private final Symbol symbol;
  private final int index;
  private final OffsetRange<CharOffset> originalTextContentRange;

  // this should only be called by a TokenSequence builder which also
  // calls setOwningTokenSequence()
  Token(int idx, Symbol symbol, OffsetRange<CharOffset> originalTextContentRange) {
    this.symbol = checkNotNull(symbol);
    this.originalTextContentRange = checkNotNull(originalTextContentRange);
    checkArgument(idx >= 0);
    this.index = idx;
  }

  public Symbol symbol() {
    return symbol;
  }

  public int index() {
    return index;
  }

  /**
   * The range of offsets in the document's original text content string covered by this token.
   */
  public OffsetRange<CharOffset> originalTextContentRange() {
    return originalTextContentRange;
  }

  /**
   * Same as {@link #index} with a little extra type-safety
   */
  public TokenOffset tokenIndex() {
    return TokenOffset.asTokenOffset(index);
  }

  /**
   * Returns the token sequence of the token this sentence belongs to.
   * @return
   */
  public TokenSequence tokenSequence() {
    return tokenSequence;
  }

  public boolean precedes(Token other) {
    return tokenSequence().sentenceIndex() < other.tokenSequence().sentenceIndex()
        || (isInTheSameSentenceAs(other) && index() < other.index());
  }

  public boolean precedesOrIs(Token other) {
    return tokenSequence().sentenceIndex() < other.tokenSequence().sentenceIndex()
        || (isInTheSameSentenceAs(other) && index() <= other.index());
  }

  public boolean follows(Token other) {
    return tokenSequence().sentenceIndex() > other.tokenSequence().sentenceIndex()
        || (isInTheSameSentenceAs(other) && index() > other.index());
  }

  public boolean followsOrIs(Token other) {
    return tokenSequence().sentenceIndex() > other.tokenSequence().sentenceIndex()
        || (isInTheSameSentenceAs(other) && index() >= other.index());
  }

  public boolean isInTheSameSentenceAs(Token other) {
    return tokenSequence().sentenceIndex() == other.tokenSequence().sentenceIndex();
  }

  /**
   * @deprecated Prefer {@link #tokenizedText()}.
   */
  @Deprecated
  public UnicodeFriendlyString text() {
    return symbol().asUnicodeFriendlyString();
  }

  /**
   * Returns the text of this as a token, which may not correspond to the original text. For
   * example, you may see "[" as "-RRB-.
   */
  public UnicodeFriendlyString tokenizedText() {
    return symbol().asUnicodeFriendlyString();
  }

  /**
   * Gets the unaltered string of the original document text covered by this {@code Token}. Note
   * that this may contain, for example, ignored markup. If the original text of the document is not
   * available, returns {@link com.google.common.base.Optional#absent()}.
   */
  public UnicodeFriendlyString rawOriginalText() {
    // we can skip creating the intermediate span object if it ever slows us down
    return tokenSequence.span(this).rawOriginalText();
  }

  /**
   * Gets the {@link com.bbn.bue.common.strings.LocatedString} of the original document text covered
   * by this {@code Token}. If the original text of the document is not available, returns {@link
   * com.google.common.base.Optional#absent()}. Unless you need the additional offset information a
   * {@link com.bbn.bue.common.strings.LocatedString} provides, prefer {@link #rawOriginalText()}.
   */
  public LocatedString originalText() {
    // we can skip creating the intermediate span object if it ever slows us down
    return tokenSequence().span(this).originalText();
  }

  /**
   * Returns the token before this one. Will return {@link com.google.common.base.Optional#absent()}
   * at the beginning of a sentence.
   */
  public Optional<Token> previousToken() {
    if (index > 0) {
      return Optional.of(tokenSequence.token(index - 1));
    } else {
      return Optional.absent();
    }
  }

  /**
   * Returns the token after this one in a sentence. Will return {@link
   * com.google.common.base.Optional#absent()} at the end of a sentence.
   */
  public Optional<Token> nextToken() {
    final int nextTokIdx = index + 1;
    if (nextTokIdx < tokenSequence.size()) {
      return Optional.of(tokenSequence.token(nextTokIdx));
    } else {
      return Optional.absent();
    }
  }

  /**
   * Returns the token at the specified index relative to this one, if available.
   * For example, supplying a value of {@code -2} for {@code relativeIndex} will
   * return the token before the token before this token, if possible.
   */
  public Optional<Token> shift(int relativeIndex) {
    int idx = index() + relativeIndex;
    if (idx >= 0 && idx < tokenSequence().size()) {
      return Optional.of(tokenSequence().token(idx));
    } else {
      return Optional.absent();
    }
  }

  @Override
  public String toString() {
    return symbol.asString() +"[" + originalTextContentRange() + "]";
  }


  // this is called only by the TokenSequence builder to set up the back-links in a
  // safely immutable way
  /* package-private */ void setOwningTokenSequence(final TokenSequence ts) {
    if (tokenSequence == null) {
      this.tokenSequence = checkNotNull(ts);
    } else {
      throw new RuntimeException("Cannot set the token sequence of a token more than once");
    }
  }

  @Override
  public Token token() {
    return this;
  }

  @Override
  public TokenSequence.Span span() {
    return tokenSequence().span(index, index);
  }

  @Override
  public TokenSpan tokenSpan() {
    return tokenSequence().span(index, index);
  }

  private enum SymbolFunction implements Function<Token, Symbol> {
    INSTANCE;

    @Override
    public Symbol apply(final Token input) {
      return input.symbol();
    }
  }

  public static Function<Token, Symbol> symbolFunction() {
    return SymbolFunction.INSTANCE;
  }

  @Override
  public int hashCode() {
    // note the hashcode does not include tokenSequence in order to avoid an infinite loop with
    // TokenSequence's hashCode!
    return Objects.hash(symbol, originalTextContentRange(), index);
  }

  @Override
  public boolean equals(final Object obj) {
    // note that equality does not look at the token sequence object to avoid an infinite loop
    // with TokenSequence's equality
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Token other = (Token) obj;
    return Objects.equals(this.symbol, other.symbol)
        && Objects.equals(this.originalTextContentRange(), other.originalTextContentRange())
        && Objects.equals(this.index, other.index);
  }

  // Deprecated methods


  @Deprecated
  public static Function<Token, UnicodeFriendlyString> Text =
      new Function<Token, UnicodeFriendlyString>() {
    @Override
    public UnicodeFriendlyString apply(final Token tok) {
      return tok.text();
    }
  };


  public OffsetGroup startOffsetGroup() {
    return tokenSequence().documentOriginalText()
        .startReferenceOffsetsForContentOffset(startCharOffset());
  }

  public OffsetGroup endOffsetGroup() {
    return tokenSequence().documentOriginalText()
        .endReferenceOffsetsForContentOffset(endCharOffset());
  }

  public EDTOffset startEDTOffset() {
    return startOffsetGroup().edtOffset();
  }

  public EDTOffset endEDTOffset() {
    return endOffsetGroup().edtOffset();
  }

  public CharOffset startCharOffset() {
    return originalTextContentRange().startInclusive();
  }

  public CharOffset endCharOffset() {
    return originalTextContentRange().endInclusive();
  }

  public Optional<ByteOffset> startByteOffset() {
    return startOffsetGroup().byteOffset();
  }

  public Optional<ByteOffset> endByteOffset() {
    return endOffsetGroup().byteOffset();
  }

  public Optional<ASRTime> startASRTime() {
    return startOffsetGroup().asrTime();
  }

  public Optional<ASRTime> endASRTime() {
    return endOffsetGroup().asrTime();
  }

  public OffsetRange<CharOffset> charOffsetRange() {
    return OffsetRange.fromInclusiveEndpoints(startCharOffset(), endCharOffset());
  }
}
