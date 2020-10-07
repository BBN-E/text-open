package com.bbn.serif.theories;

import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.EDTOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.theories.TokenSequence.Span;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for working with objects implementing the Spanning interface.
 *
 * @author rgabbard
 */
public final class Spannings {

  private Spannings() {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Prefer {@link #toSpanFunction()}.
   */
  @Deprecated
  public static Function<Spanning, TokenSequence.Span> ToSpan =
      ToSpanFunction.INSTANCE;

  public static Function<Spanning, TokenSequence.Span> toSpanFunction() {
    return ToSpanFunction.INSTANCE;
  }

  /**
   * Distance between the spans of two spannings.  Simply calls Spans.distance on their spans.
   */
  public static int distance(final Spanning a, final Spanning b) {
    final Span spanA = a.span();
    final Span spanB = b.span();

    return Spans.distance(spanA, spanB);
  }

  /**
   * Returns an abbreviate representation of the text between two spans. First, a must precede b and
   * not overlap; otherwise an exception is thrown. We return a list of Spannings between a and b,
   * where each is either a preterminal SynNode, a Mention, or a ValueMention.  You can imagine the
   * initial return value is the sequence of Tokens between a and b, but then each span of tokens
   * within a Mention or ValueMention is replaced by that Mention or ValueMention. Then remaining
   * tokens are replaced by their preterminal SynNodes.
   *
   * If the left input Spanning is within a Mention or ValueMention, that Mention or valueMention is
   * ignored.  However, if the right input Spanning is within a Mention or ValueMention and is not
   * on the left edge, it will be included.
   */
  public static List<Spanning> abbreviatedSpansBetween(final DocTheory dt, final Spanning a,
      final Spanning b) {
    checkNotNull(a);
    checkNotNull(b);
    final Span left = a.span();
    final Span right = b.span();
    checkArgument(left.endsBefore(right));

    final ImmutableList.Builder<Spanning> ret = ImmutableList.builder();
    final int endIdx = right.startIndex();
    final TokenSequence ts = right.tokenSequence();
    final SentenceTheory st = dt.sentenceTheory(ts.sentenceIndex());

    int curIdx = left.endIndex() + 1;

    while (curIdx < endIdx) {
      final Token tok = ts.token(curIdx);
      final Optional<Mention> mention =
          st.mentions().longestRecognizedMentionBeginningAt(tok);
      final Optional<ValueMention> valueMention =
          st.valueMentions().longestValueMentionBeginningAt(tok);

      if (mention.isPresent()) {
        ret.add(mention.get());
        curIdx = mention.get().span().endIndex() + 1;
      } else if (valueMention.isPresent()) {
        ret.add(valueMention.get());
        curIdx = valueMention.get().span().endIndex() + 1;
      } else {
        ret.add(st.parse().nodeForToken(tok));
        ++curIdx;
      }
    }

    return ret.build();
  }

  public static boolean overlaps(final Spanning a, final Spanning b) {
    return a.span().overlaps(b.span());
  }

  public static boolean precedes(final Spanning a, final Spanning b) {
    return a.span().endsBefore(b.span());
  }

  public static Function<Spanning, OffsetRange<CharOffset>> toCharOffsetRange() {
    return ToCharOffsetRange.INSTANCE;
  }

  /**
   * An {@link Ordering} over {@link Spanning}s by the start EDT offset. This may be applied between
   * {@code Spannings} in different sentences, but is undefined when applied between those in
   * different documents.
   */
  public static Ordering<Spanning> ByStartEDTOffset = Ordering.natural().onResultOf(
      new Function<Spanning, EDTOffset>() {
        @Override
        public EDTOffset apply(final Spanning s) {
          return s.span().startEDTOffset();
        }
      });

  /**
   * An {@link Ordering} over {@link Spanning}s by the length of the span in characters according to
   * EDT offsets (so this ignores certain omitted markup in the original source document). This may
   * be applied between {@code Spannings} in different sentences or even different documents.
   */
  public static Ordering<Spanning> ByEDTOffsetLength = Ordering.natural().onResultOf(
      new Function<Spanning, Integer>() {
        @Override
        public Integer apply(final Spanning s) {
          return s.span().endEDTOffset().asInt() - s.span().startEDTOffset().asInt();
        }
      });

  /**
   * An ordering where things earlier in the document by start token EDT offset are ranked higher.
   */
  public static final Ordering<Spanning> EarliestByEDTOffset =
      // reverse of first offset means maximum is earliest
      Spannings.ByStartEDTOffset.reverse();


  /**
   * An {@link Ordering} over {@link Spanning}s which prefers the earliest by EDT offset and breaks
   * ties by preffering longer spannings. This may be applied between {@code Spannings} in different
   * sentences, but is undefined when applied between those in different documents.
   */
  public static final Ordering<Spanning> EarliestThenLongest =
      EarliestByEDTOffset
          // break ties by length, longest is maximum
          .compound(Spannings.ByEDTOffsetLength);

  /**
   * Order {@code Spanning}s by their proximity to a provided focus, measured in sentences. Result
   * undefined if applied to {@code Spanning}s from different documents.
   *
   * @param focusSpanning May not be null.
   */
  public static Ordering<Spanning> InCloserSentenceTo(final Spanning focusSpanning) {
    return Ordering.natural().onResultOf(new Function<Spanning, Integer>() {
      @Override
      public Integer apply(final Spanning s) {
        return Math.abs(s.span().sentenceIndex() - focusSpanning.span().sentenceIndex());
      }
    })
        // to make max closer
        .reverse();
  }

  /**
   * @deprecated Prefer {@link #tokenizedTextFunction()}
   */
  @Deprecated
  public static final Function<Spanning, UnicodeFriendlyString> TokenizedText =
      new Function<Spanning, UnicodeFriendlyString>() {
    @Override
    public UnicodeFriendlyString apply(final Spanning input) {
      return input.span().tokenizedText();
    }
  };

  /**
   * See {@link com.bbn.serif.theories.TokenSequence.Span#tokenizedText()}.
   */
  public static Function<Spanning, UnicodeFriendlyString> tokenizedTextFunction() {
    return TokenizedTextFunction.INSTANCE;
  }

  private enum TokenizedTextFunction implements Function<Spanning, UnicodeFriendlyString> {
    INSTANCE {
      @Override
      public UnicodeFriendlyString apply(final Spanning input) {
        return input.span().tokenizedText();
      }
    }
  }


  public static final Function<Spanning, Integer> sentenceIndexFunction() {
    return Functions.compose(Spans.sentenceIndexFunction(), toSpanFunction());
  }

  private enum ToSpanFunction implements Function<Spanning, TokenSequence.Span> {
    INSTANCE;

    @Override
    public Span apply(@Nullable final Spanning spanning) {
      return checkNotNull(spanning).span();
    }
  }


  private enum ToCharOffsetRange implements Function<Spanning, OffsetRange<CharOffset>> {
    INSTANCE;

    @Override
    public OffsetRange<CharOffset> apply(final Spanning input) {
      return input.span().charOffsetRange();
    }
  }
}
