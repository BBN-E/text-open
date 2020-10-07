package com.bbn.serif.theories;

import com.bbn.bue.common.StringNormalizer;
import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents a sequence of tokens for a single sentence.
 */
public final class TokenSequence
    implements Iterable<Token>, Spanning, PotentiallyAbsentSerifTheory {

  private static final TokenSequence ABSENT =
      new TokenSequence(
          LocatedString.fromReferenceString("Bogus original text for absent sentence theory!"),
          Collections.<Token>emptyList(), 0, 0f, true);

  private TokenSequence(LocatedString originalText, final Iterable<Token> tokens,
      final int sentenceIndex, final float score, final boolean absent) {
    this.originalText = checkNotNull(originalText);
    this.tokens = ImmutableList.copyOf(tokens);
    this.sentenceIndex = sentenceIndex;
    checkArgument(sentenceIndex >= 0, "Token sequence may not have a negative sentence index");
    this.score = score;
    this.absentFlag = absent;

    // if we know the original text, use it to ensure token offsets are valid
    // ideally we'd like to check they are within sentence-bounds but we don't know
    // about the sentence boundaries here
    for (final Token tok : tokens) {
      checkArgument(
          originalText.referenceBounds().asCharOffsetRange().contains(tok.charOffsetRange()),
          "Token %s is outside original text character offset boundaries %s",
          tok, originalText.referenceBounds().asCharOffsetRange());
    }
  }

  // TODO: currently we count an empty token sequence as absent, which is not quite right.
  // I'm not entirely sure why the isAbsent() methods are here anyhow
  @Override
  public boolean isAbsent() {
    return absentFlag;
  }

  public static TokenSequence absent(int sentenceIndex) {
    return ABSENT;
  }

  public float score() {
    return score;
  }

  public SentenceTheory sentenceTheory(final DocTheory dt) {
    return dt.sentenceTheory(sentenceIndex);
  }

  /**
   * Returns the original text of the document this token sequence belongs to.
   */
  public LocatedString documentOriginalText() {
    return originalText;
  }

  public int sentenceIndex() {
    return sentenceIndex;
  }

  public int size() {
    return tokens.size();
  }

  @Override
  public Iterator<Token> iterator() {
    return tokens.iterator();
  }

  public Token token(final int idx) {
    return tokens.get(idx);
  }

  public boolean isEmpty() {
    return size() == 0;
  }

  public UnicodeFriendlyString text() {
    return span().text();
  }

  /**
   * Use just plain span()
   */
  @Deprecated
  public Span fullSpan() {
    return span();
  }

  @Override
  public Span span() {
    if (isEmpty()) {
      throw new UnsupportedOperationException("Cannot get the span of an empty token sequence.");
    } else {
      return this.span(0, size() - 1);
    }
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  public Optional<Integer> tokenIndexStartingAt(final CharOffset offset) {
    for (int idx = 0; idx < size(); ++idx) {
      if (token(idx).startCharOffset().equals(offset)) {
        return Optional.of(idx);
      }
    }
    return Optional.absent();
  }

  public Optional<Integer> tokenIndexEndingAt(final CharOffset offset) {
    for (int idx = 0; idx < size(); ++idx) {
      if (token(idx).endCharOffset().equals(offset)) {
        return Optional.of(idx);
      }
    }
    return Optional.absent();
  }

  public Span span(final int startInclusive, final int endInclusive) {
    checkArgument(startInclusive >= 0);
    checkArgument(endInclusive < size());
    checkArgument(startInclusive <= endInclusive);
    return new Span(startInclusive, endInclusive);
  }

  private Span span(Token startToken, Token endToken) {
    return span(startToken.index(), endToken.index());
  }


  public Span span(final Token tok) {
    return span(tok.index(), tok.index());
  }

  @SuppressWarnings("ReferenceEquality")
  public Span maxSpan(final List<Span> spans) {
    checkArgument(!spans.isEmpty());

    int startIndex = spans.get(0).startIndex();
    int endIndex = spans.get(0).endIndex();

    for (final Span span : spans) {
      // we suppress the warning for this because we do required them to be from exactly the same
      // token sequence object
      if (span.tokenSequence() != this) {
        throw new IllegalArgumentException("Spans must all be from the same TokenSequence");
      }
      startIndex = Math.min(startIndex, span.startIndex());
      endIndex = Math.max(endIndex, span.endIndex());
    }

    return span(startIndex, endIndex);
  }

  /**
   * Gets the span of tokens in this token sequence such that the character offset of the starting
   * token exactly matches the starting character offset of {@code charOffsetRange} and the ending
   * character offset of the ending token exactly matches the ending character offset of {@code
   * charOffsetRange}. If no such token span exists, returns {@link Optional#absent}.
   */
  public Optional<Span> spanFromCharacterOffsets(OffsetRange<CharOffset> charOffsetOffsetRange) {
    Token startToken = null;
    Token endToken = null;

    for (final Token tok : tokens) {
      if (tok.startCharOffset().equals(charOffsetOffsetRange.startInclusive())) {
        startToken = tok;
      }
      if (tok.endCharOffset().equals(charOffsetOffsetRange.endInclusive())) {
        endToken = tok;
      }
    }

    if (startToken != null && endToken != null) {
      return Optional.of(span(startToken, endToken));
    } else {
      return Optional.absent();
    }
  }

  public Optional<Span> smallestSpanContaining(final OffsetRange<CharOffset> offsetRange) {
      checkArgument(
          documentOriginalText().referenceBounds().startCharOffsetInclusive().asInt() <= offsetRange
              .startInclusive().asInt(),
          "cannot have an input offset starting before the original text!");
      checkArgument(
          documentOriginalText().referenceBounds().endCharOffsetInclusive().asInt() >= offsetRange
              .endInclusive().asInt(),
          "cannot have an input offset ending after the original text!");

    final CharOffset lower = offsetRange.startInclusive();
    final CharOffset higher = offsetRange.endInclusive();
    Token startToken = null;
    Token endToken = null;
    for (final Token tok : tokens) {

      if (tok.charOffsetRange().asRange().contains(lower)) {
        // contained within a token
        startToken = tok;
      } else if (startToken == null && tok.endCharOffset().compareTo(lower) > 0) {
        // happens if an offset is between tokens, only happens once in this loop
        startToken = tok;
      }
      if (tok.charOffsetRange().asRange().contains(higher)) {
        // contained within a token
        endToken = tok;
      } else if (endToken == null && tok.startCharOffset().compareTo(higher) > 0) {
        // happens if an offset is between tokens, only happens once in this loop
        endToken = tok;
      }
    }
    if (startToken != null && endToken != null) {
      return Optional.of(span(startToken, endToken));
    } else {
      return Optional.absent();
    }
  }

  /**
   * A contiguous span of tokens within a token sequence.  It is always tied to a particular token
   * sequence.
   *
   * @author rgabbard
   */
  public class Span extends AbstractTokenSpan implements TokenSpan, Iterable<Token> {

    public int startIndex() {
      return start;
    }

    public int endIndex() {
      return end;
    }

    public int size() {
      return end - start + 1;
    }

    public TokenSequence tokenSequence() {
      return TokenSequence.this;
    }

    @Deprecated
    public Token firstToken() {
      return token(start);
    }

    public SentenceTheory sentenceTheory(final DocTheory docTheory) {
      return tokenSequence().sentenceTheory(docTheory);
    }

    /**
     * Get the tokens between this Span and the specified span.
     *
     * @param later The Span to get tokens up to.  Must be from the same token sequence as this span
     *              and must strictly follow it.
     * @return An Optional containing the token span strictly between this Span and <tt>later</tt>.
     * If there are no such tokens, <tt>Optional.absent()</tt> is returned.
     */
    @SuppressWarnings("ReferenceEquality")
    public Optional<TokenSequence.Span> until(final TokenSequence.Span later) {
      checkNotNull(later);
      checkArgument(tokenSequence() == later.tokenSequence());
      checkArgument(endIndex() < later.startIndex());

      if (endIndex() + 1 == later.startIndex()) {
        return Optional.absent();
      }
      return Optional.of(tokenSequence().span(endIndex() + 1, later.startIndex() - 1));
    }


    /**
     * Gets a token span preceding this Span.
     *
     * @param context How many preceding tokens to return. If this many tokens are not available, it
     *                will return as many as possible.
     * @return An <tt>Optional</tt> wrapping a <tt>Span</tt> representing the <tt>context</tt>
     * preceding tokens.  If there are no preceding tokens, <tt>Optional.absent()</tt> is returned.
     */
    public Optional<TokenSequence.Span> precedingTokens(final int context) {
      checkArgument(context >= 0);
      if (context == 0 || start == 0) {
        return Optional.absent();
      }
      final int newStart = Math.max(0, start - context);
      return Optional.of(tokenSequence().span(newStart, start - 1));
    }

    /**
     * Gets a token span following this Span.
     *
     * @param context How many following tokens to return. If this many tokens are not available, it
     *                will return as many as possible.
     * @return An <tt>Optional</tt> wrapping a <tt>Span</tt> representing the <tt>context</tt>
     * following tokens.  If there are no following tokens, <tt>Optional.absent()</tt> is returned.
     */
    public Optional<TokenSequence.Span> followingTokens(final int context) {
      checkArgument(context >= 0);

      if (context == 0 || end == tokenSequence().size() - 1) {
        return Optional.absent();
      }
      final int newEnd = Math.min(end + context, tokenSequence().size() - 1);
      return Optional.of(tokenSequence().span(end + 1, newEnd));
    }

    public int sentenceIndex() {
      return tokenSequence().sentenceIndex();
    }

    public Span extend(final int numWords) {
      checkArgument(numWords > 0);
      return tokenSequence().span(Math.max(0, start - numWords),
          Math.min(tokenSequence().size() - 1, end + numWords));
    }

    public Span extendLeft(final int numWords) {
      checkArgument(numWords > 0);
      return tokenSequence().span(Math.max(0, start - numWords), end);
    }

    public Span extendRight(final int numWords) {
      checkArgument(numWords > 0);
      return tokenSequence().span(start, Math.min(tokenSequence().size() - 1, end + numWords));
    }

    /**
     * Returns the number of tokens between two spans in the same sentence (0 if they overlap). Will
     * throw an exception if the spans are not in the same sentnece.
     */
    public int numberOfTokensBetweenSpansInSameSentence(final Span span) {
      checkArgument(span.sentenceIndex() == sentenceIndex());
      if (span.overlaps(this)) {
        return 0;
      }
      if (span.endsBefore(this)) {
        return startIndex() - span.endIndex();
      }
      if (span.startsAfter(this)) {
        return span.startIndex() - this.endIndex();
      }
      throw new RuntimeException("Should be impossible.");
    }

    /* Returns the tokens of this Span as Strings joined by single spaces.
     */
    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();

      for (int i = start; i <= end; ++i) {
        sb.append(TokenSequence.this.token(i).tokenizedText());
        if (i != end) {
          sb.append(" ");
        }
      }

      return sb.toString();
    }

    /**
     * Deprecated for clarity. Use tokenizedText(). *
     */
    @Deprecated
    public UnicodeFriendlyString text() {
      return tokenizedText();
    }

    /**
     * Returns the tokens of this span, joined by spaces. Note that this will not in general
     * correspond exactly to the text in the original document.
     *
     * Returns "" for an empty span.
     */
    public UnicodeFriendlyString tokenizedText() {
      final List<String> parts = Lists.newArrayList();

      for (int i = start; i <= end; ++i) {
        parts.add(TokenSequence.this.token(i).tokenizedText().utf16CodeUnits());
      }

      // we can safely concatenate the code points regardless of whether it's all BMP
      // or has some non-BMP characters
      return StringUtils.unicodeFriendly(StringUtils.spaceJoiner().join(parts));
    }

    private Span(final int start, final int end) {
      checkArgument(start >= 0);
      checkArgument(end >= start);
      this.start = start;
      this.end = end;
    }

    private final int start;
    private final int end;

    @Override
    public TokenSequence startTokenSequence() {
      return tokenSequence();
    }

    @Override
    public TokenSequence endTokenSequence() {
      return tokenSequence();
    }

    @Override
    public int startTokenIndexInclusive() {
      return startIndex();
    }

    @Override
    public int endTokenIndexInclusive() {
      return endIndex();
    }

    @Override
    public Iterator<Token> iterator() {
      return new SpanIterator();
    }

    private final class SpanIterator extends AbstractIterator<Token> {

      int idx = start;

      @Override
      protected Token computeNext() {
        if (idx <= end) {
          final Token ret = TokenSequence.this.token(idx);
          ++idx;
          return ret;
        } else {
          return endOfData();
        }
      }
    }
  }

  @SuppressWarnings("ReferenceEquality")
  private static void checkAllFromTheSameSentence(final Iterable<TokenSequence.Span> spans) {
    checkArgument(!Iterables.isEmpty(checkNotNull(spans)));
    final TokenSequence.Span firstSpan = Iterables.get(spans, 0);

    for (final TokenSequence.Span span : spans) {
      checkArgument(firstSpan.tokenSequence() == span.tokenSequence(),
          "Spans must all be from the same sentence.");
    }
  }

  @SuppressWarnings("ReferenceEquality")
  public static TokenSequence.Span union(final TokenSequence.Span left,
      final TokenSequence.Span right) {
    checkArgument(checkNotNull(left).tokenSequence() == checkNotNull(right).tokenSequence());
    return left.tokenSequence().span(Math.min(left.startIndex(), right.startIndex()),
        Math.max(left.endIndex(), right.endIndex()));
  }

  public static TokenSequence.Span union(final Iterable<TokenSequence.Span> spans) {
    checkArgument(!Iterables.isEmpty(checkNotNull(spans)));
    checkAllFromTheSameSentence(spans);
    final TokenSequence.Span firstSpan = Iterables.get(spans, 0);
    int start = firstSpan.startIndex();
    int end = firstSpan.endIndex();
    for (final TokenSequence.Span span : spans) {
      if (span.startIndex() < start) {
        start = span.startIndex();
      }
      if (span.endIndex() > end) {
        end = span.endIndex();
      }
    }
    return firstSpan.tokenSequence().span(start, end);
  }

  /**
   * A comparator which compares first by TokenSequence.Spans by sentence index, then start token
   * index, then end token index. Empty spans are sorted first.
   *
   * @author rgabbard
   */
  public static final Comparator<TokenSequence.Span> Lexicographic =
      new Comparator<TokenSequence.Span>() {
        @Override
        public int compare(final Span o1, final Span o2) {
          checkNotNull(o1);
          checkNotNull(o2);
          return ComparisonChain.start()
              .compare(o1.tokenSequence().sentenceIndex(), o2.tokenSequence().sentenceIndex())
              .compare(o1.startIndex(), o2.startIndex())
              .compare(o1.endIndex(), o2.endIndex()).result();
        }
      };

  public static Predicate<TokenSequence.Span> StartsBefore(final Span other) {
    return new Predicate<TokenSequence.Span>() {
      @Override
      public boolean apply(final TokenSequence.Span x) {
        return x.startsBefore(other);
      }
    };
  }

  public static Predicate<TokenSequence.Span> StartsAfter(final Span other) {
    return new Predicate<TokenSequence.Span>() {
      @Override
      public boolean apply(final TokenSequence.Span x) {
        return x.startsAfter(other);
      }
    };
  }

  public static Predicate<TokenSequence.Span> EndsBefore(final Span other) {
    return new Predicate<TokenSequence.Span>() {
      @Override
      public boolean apply(final TokenSequence.Span x) {
        return x.endsBefore(other);
      }
    };
  }

  public static Predicate<TokenSequence.Span> EndsAfter(final Span other) {
    return new Predicate<TokenSequence.Span>() {
      @Override
      public boolean apply(final TokenSequence.Span x) {
        return x.endsAfter(other);
      }
    };
  }

  @Override
  public String toString() {
    return "[" + sentenceIndex + " -> " + FluentIterable.from(tokens).transform(
        Functions.toStringFunction()).join(StringUtils.spaceJoiner()) + "]";
  }

  private final ImmutableList<Token> tokens;
  // nullable
  private final LocatedString originalText;
  private final int sentenceIndex;
  private final float score;
  private final boolean absentFlag;

  /**
   * Creates a token sequence which knows the original text of the document it came from. {@code
   * originalText} is the original text of the entire document, not this sentence.
   */
  public static FromTokenDataBuilder withOriginalText(int sentenceIndex,
      LocatedString originalText) {
    return new FromTokenDataBuilder(checkNotNull(originalText), sentenceIndex);
  }

  public static final class FromTokenDataBuilder {

    private final ImmutableList.Builder<Token> tokensBuilder = ImmutableList.builder();
    private final LocatedString originalText;
    private float score = 1.0f;
    private final int sentenceIndex;
    private int tokensAdded = 0;
    private Token lastTokenAdded = null;

    private FromTokenDataBuilder(final LocatedString originalText, final int sentenceIndex) {
      // nullable
      this.originalText = originalText;
      this.sentenceIndex = sentenceIndex;
    }

    public FromTokenDataBuilder addToken(Symbol tokSymbol, OffsetRange<CharOffset> originalTextContentRange) {
      tokensBuilder
          .add(lastTokenAdded = new Token(tokensAdded++, tokSymbol, originalTextContentRange));
      return this;
    }

    Optional<Token> lastTokenAdded() {
      return Optional.fromNullable(lastTokenAdded);
    }

    public FromTokenDataBuilder setScore(float score) {
      this.score = score;
      return this;
    }

    public TokenSequence build() {
      final ImmutableList<Token> tokens = tokensBuilder.build();
      final TokenSequence ret = new TokenSequence(originalText, tokens, sentenceIndex, score, false);
      for (final Token tok : tokens) {
        tok.setOwningTokenSequence(ret);
      }
      return ret;
    }
  }

  /**
   * Uses the tokenization from the primary sentence theory for each sentence
   */
  public static Function<DocTheory, Integer> docTheoryToTokenCountFunction() {
    return DocTheoryToTokenCount.INSTANCE;
  }

  private static enum DocTheoryToTokenCount implements Function<DocTheory,Integer> {
    INSTANCE;
    @Override
    public Integer apply(final DocTheory input) {
      int counter = 0;
      for(SentenceTheory sentenceTheory : input.nonEmptySentenceTheories()) {
        counter += sentenceTheory.tokenSequence().size();
      }
      return counter;
    }

  }

  /**
   * Gets a token sequence which is identical to this one except with each token transformed by the
   * given {@link StringNormalizer}.  If the normalizer makes no changes, this {@code TokenSequence}
   * itself will be returned.
   */
  public TokenSequence copyWithTransformedTokens(StringNormalizer normalizer) {
    final FromTokenDataBuilder ret = withOriginalText(sentenceIndex, originalText);
    boolean changed = false;
    for (final Token origTok : tokens) {
      final Symbol normSymbol = Symbol.from(normalizer.normalize(origTok.symbol().asString()));
      changed = changed || !normSymbol.equalTo(origTok.symbol());
      ret.addToken(normSymbol, origTok.originalTextContentRange());
    }
    return changed ? ret.build() : this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(tokens, originalText, sentenceIndex, score, absentFlag);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TokenSequence other = (TokenSequence) obj;
    return Objects.equals(this.tokens, other.tokens)
        && Objects.equals(this.originalText, other.originalText)
        && Objects.equals(this.sentenceIndex, other.sentenceIndex)
        && Objects.equals(this.score, other.score)
        && Objects.equals(this.absentFlag, other.absentFlag);
  }

}
