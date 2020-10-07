package com.bbn.serif.theories;

import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.EDTOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.strings.offsets.OffsetRange;

import com.google.common.base.Optional;

import java.util.List;

/**
 * Represents a span of tokens which may cross sentences.  There are two ways to obtain one of
 * these: from {@link com.bbn.serif.theories.TokenSequence#span()} and from {@link
 * TokenSpans#from(Token)}.  Please do not create your own implementations of this
 * interface. It is unnecessary and could cause undefined results.
 *
 * If an object can provide you with a {@code TokenSpan}, it will implement {@link
 * com.bbn.serif.theories.TokenSpanning}.
 *
 * Two {@code TokenSpan}s are equal iff their start token sequences, end token sequences, start
 * indices, and end indices are equal.
 */
public interface TokenSpan {

  /**
   * The token sequence containing the start token for this span. Will never be null.
   */
  TokenSequence startTokenSequence();

  /**
   * The token sequence containing the end token for this span. Will never be null.
   */
  TokenSequence endTokenSequence();

  /**
   * The index (inclusive) of the first token in the span, relative to its containing {@link
   * com.bbn.serif.theories.TokenSequence}.
   */
  int startTokenIndexInclusive();

  /**
   * The index (inclusive) of the last token in the span, relative to its containing {@link
   * com.bbn.serif.theories.TokenSequence}.
   */
  int endTokenIndexInclusive();

  /**
   * Returns whether or not this span is contained in a single sentence.
   */
  boolean inSingleSentence();

  int startSentenceIndex();

  int endSentenceIndex();

  boolean overlapsSentenceIndex(int sentIndex);

  // token accessors
  boolean isSingleToken();

  Token startToken();

  Token endToken();

  // offset accessors

  /**
   * Starting codepoint offset of this span in the document original text.
   */
  CharOffset startCharOffset();


  OffsetGroup startOffsetGroup();

  OffsetGroup endOffsetGroup();

  /**
   * Ending codepoint offset of this span in the document original text.
   */
  CharOffset endCharOffset();

  EDTOffset startEDTOffset();

  EDTOffset endEDTOffset();

  /**
   * Codepoint offset range of this span in the document original text.
   */
  OffsetRange<CharOffset> charOffsetRange();

  OffsetGroupRange offsetRange();


  // comparison methods
  boolean contains(TokenSpan other);

  boolean overlaps(TokenSpan other);

  /**
   * @return whether this span starts strictly before <tt>other</tt>.
   */
  boolean startsBefore(TokenSpan other);

  /**
   * @return whether this span ends stricly before <tt>other</tt>
   */
  boolean endsBefore(TokenSpan other);

  /**
   * @return Whether this span starts strictly after <tt>other</tt>.
   */
  boolean startsAfter(TokenSpan other);

  /**
   * @return Whether this {@link TokenSpan} ends stricly after <tt>other</tt>/
   */
  boolean endsAfter(TokenSpan other);

  /**
   * Returns true iff this token span and {@code code} are both contained within the same single
   * sentence.  Note this will return false if either is a multi-sentence span.
   */
  boolean containedInSameSentenceAs(TokenSpan other);

  /**
   * Gets the unaltered string of the original document text covered by this {@link com.bbn.serif.theories.TokenSpan}.
   * Note that this may contain, for example, ignored markup.
   * If the original text of the document is not available, returns {@link com.google.common.base.Optional#absent()}.
   */
  UnicodeFriendlyString rawOriginalText();

  /**
   * Gets the {@link com.bbn.bue.common.strings.LocatedString} of the original document text
   * covered by this {@link com.bbn.serif.theories.TokenSpan}. If the original text of the
   * document is not available, returns {@link com.google.common.base.Optional#absent()}. Unless
   * you need the additional offset information a {@link com.bbn.bue.common.strings.LocatedString}
   * provides, prefer {@link #rawOriginalText()}.
   */
  LocatedString originalText();

  List<Token> tokens(DocTheory dt);

  int numTokens(DocTheory dt);

  /**
   * Returns the tokens of this span, joined by spaces. Note that this will not in general
   * correspond exactly to the text in the original document. The tokenizations used for the
   * sentences containing the endpoints is that of the token sequences stored on this {@code
   * TokenSpan}. The tokenizations of intervening sentences are drawn from the primary tokenizations
   * from the supplied {@link com.bbn.serif.theories.DocTheory}.
   */
  UnicodeFriendlyString tokenizedText(DocTheory dt);

  // deprecated methods

  /**
   * Prefer {@link #rawOriginalText()}. Its name is clearer and it doesn't require passing the
   * {@link com.bbn.serif.theories.DocTheory}.
   *
   * @deprecated
   */
  @Deprecated
  Optional<UnicodeFriendlyString> originalText(DocTheory dt);

}
