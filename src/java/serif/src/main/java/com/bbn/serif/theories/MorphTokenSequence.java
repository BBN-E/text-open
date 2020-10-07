package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Provides morphological analyses for the tokens of a sentence.  This is provide as a sequence
 * of {@link MorphToken}s, each of which may span multiple contiguous tokens to handle periphrastic
 * forms, etc..
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@Functional
@JsonSerialize(as = ImmutableMorphTokenSequence.class)
@JsonDeserialize(as = ImmutableMorphTokenSequence.class)
public abstract class MorphTokenSequence implements Spanning {
  public abstract MorphologyAlgorithmDescription sourceAlgorithm();

  /**
   * Sequence of morphologically-analyzed groups of tokens.  It is guaranteed that these will not
   * overlap and will be in sorted over. However, no guarantee is given that all tokens are covered.
   */
  public abstract ImmutableList<MorphToken> morphTokens();
  public abstract TokenSequence tokenSequence();
  public abstract Optional<Double> score();


  @Override
  public final TokenSequence.Span span() {
    return tokenSequence().span();
  }

  @Override
  public final TokenSequence.Span tokenSpan() {
    return span();
  }

  /**
   * Returns which {@link MorphToken}, if any, covers the provided token.
   */
  public final Optional<MorphToken> morphTokenForToken(Token t) {
    return Optional.fromNullable(tokensToMorphTokens().get(t));
  }

  @Value.Check
  protected void check() {
    for (int i=1; i<morphTokens().size(); ++i) {
      checkArgument(morphTokens().get(i-1).span().endsBefore(morphTokens().get(i).span()),
          "MorphTokens in a MorphTokenSequence must be sorted and non-overlapping");
    }

    checkArgument(!score().isPresent() || sourceAlgorithm().providesSequenceScores(),
        "Score found for MorphTokenSequence, but algorithm claims not to provide them");

    final ImmutableSet<MorphologyAlgorithmDescription> algorithms =
        FluentIterable.from(morphTokens())
            .transformAndConcat(MorphTokenFunctions.analyses())
            .transform(MorphTokenAnalysisFunctions.sourceAlgorithm())
            .toSet();

    final boolean empty = algorithms.size() == 0;
    checkArgument(empty ||
        (algorithms.size() == 1 && algorithms.asList().get(0).equals(sourceAlgorithm())),
        "Multiple morphology algorithms encountered in the same MorphTokenSequence");

  }

  // for internal use by morphTokenForToken
  @Value.Lazy
  ImmutableMap<Token, MorphToken> tokensToMorphTokens() {
    final ImmutableMap.Builder<Token, MorphToken> ret = ImmutableMap.builder();

    for (final MorphToken morphToken : morphTokens()) {
      for (final Token tok : morphToken.span()) {
        ret.put(tok, morphToken);
      }
    }

    return ret.build();
  }


  public static class Builder extends ImmutableMorphTokenSequence.Builder {}
}
