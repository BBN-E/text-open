package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import org.immutables.func.Functional;
import org.immutables.value.Value;

/**
 * The minimal object implementing the Spanning interface. This is used when we map a span from one
 * DocTheory to another, since we don't know if any other object corresponds to the span in the
 * target document.
 *
 * @author rgabbard
 */
@TextGroupImmutable
@Value.Immutable
@Functional
public abstract class MinimalSpanning implements Spanning {

  public static MinimalSpanning forSpan(TokenSequence.Span span) {
    return new MinimalSpanning.Builder().span(span).build();
  }

  @Override
  public abstract TokenSequence.Span span();

  @Override
  public final TokenSpan tokenSpan() {
    return span();
  }

  @Override
  public String toString() {
    return span().toString();
  }

  static class Builder extends ImmutableMinimalSpanning.Builder {

  }
}
