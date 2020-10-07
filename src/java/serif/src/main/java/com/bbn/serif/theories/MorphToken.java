package com.bbn.serif.theories;

import com.bbn.bue.common.OptionalUtils;
import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import org.immutables.func.Functional;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Describes the morphological analysis of a contiguous span of {@link Token}s.  Typically
 * this will be a single token but may be more for periphrastic forms, etc.
 *
 * The same span of tokens may be associated with multiple {@link MorphTokenAnalysis}es.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@JsonSerialize
@JsonDeserialize
@Functional
public abstract class MorphToken implements Spanning, WithMorphToken {

  /**
   * The possible analyses provided for this token by the morphological analyzer. Ideally these
   * should be in order from best to worst.  If scores are present, they must be sorted from highest
   * score to lowest with any unscored analyses at the end.
   */
  public abstract ImmutableSet<MorphTokenAnalysis> analyses();

  @Override
  public final TokenSequence.Span tokenSpan() {
    return span();
  }

  protected void check() {
    final FluentIterable<Double> scoresInOrder = FluentIterable.from(analyses())
        .transform(MorphTokenAnalysisFunctions.score())
        .transform(OptionalUtils.deoptionalizeFunction(Double.NEGATIVE_INFINITY));

    // note sortedCopy is a stable sort
    checkArgument(scoresInOrder.equals(Ordering.natural().reverse().sortedCopy(scoresInOrder)),
        "MorphToken's analyses for span %s not in sorted order. Scores are %s:",
        span(), scoresInOrder);
  }

  public static class Builder extends ImmutableMorphToken.Builder {}

  @Override
  public String toString() {
    return tokenSpan() + "=" + analyses();
  }
}
