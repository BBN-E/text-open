package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.MinimalSpanning;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.TokenSequence;

import com.google.common.annotations.Beta;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A {@link PatternMatch} which matches some span of tokens.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class TokenSpanPatternMatch extends AbstractPatternMatch implements PatternMatch {

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public final SentenceTheory guaranteedSentenceTheory() {
    return sentenceTheory().get();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public final Spanning guaranteedSpanning() {
    return spanning().get();
  }

  @Value.Check
  protected void check() {
    checkArgument(sentenceTheory().isPresent());
    checkArgument(spanning().isPresent());
  }

  public static TokenSpanPatternMatch of(DocTheory dt, SentenceTheory st, TokenSequence.Span span) {
    return ImmutableTokenSpanPatternMatch.builder()
        .docTheory(dt).sentenceTheory(st).spanning(MinimalSpanning.forSpan(span)).build();
  }
}
