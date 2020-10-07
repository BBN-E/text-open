package com.bbn.serif.theories;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.theories.TokenSequence.Span;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Spans {

  private Spans() {
    throw new UnsupportedOperationException();
  }

  /**
   * Distance between two spans.  Distance is zero if they overlap. Otherwise, is one greater than
   * the number of tokens between them (e.g. adjacent tokens have distance 1).
   */
  public static int distance(Span a, Span b) {
    if (a.overlaps(b)) {
      return 0;
    }

    if (a.endsBefore(b)) {
      return b.startIndex() - a.endIndex();
    } else {
      return a.startIndex() - b.endIndex();
    }
  }

  /**
   * Returns a predicate which returns true if the span being tested occurs within the enclosing
   * span.
   */
  public static Predicate<Span> ContainedIn(final Span enclosingSpan) {
    checkNotNull(enclosingSpan);
    return new Predicate<Span>() {
      @Override
      public boolean apply(Span x) {
        return enclosingSpan.contains(x);
      }
    };
  }


  public static Function<Span, OffsetRange<CharOffset>> asCharOffsetRangeFunction() {
    return new Function<Span, OffsetRange<CharOffset>>() {
      @Override
      public OffsetRange<CharOffset> apply(final Span input) {
        return input.charOffsetRange();
      }
    };
  }

  public static Function<Span, Integer> sentenceIndexFunction() {
    return SentenceIndexFunction.INSTANCE;
  }

  private enum SentenceIndexFunction implements Function<Span, Integer> {
    INSTANCE {
      @Override
      public Integer apply(@Nullable final Span span) {
        return checkNotNull(span).sentenceIndex();
      }
    }
  }
}
