package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A single morphological analysis of a contiguous group of tokens. Most of the information here
 * may or may not be filled in; consult {@link MorphologyAlgorithmDescription} if you need
 * to know whether any of the collections are truly empty vs. unsupported by the populating
 * algorithm.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable(prehash = true)
@JsonSerialize
@JsonDeserialize
@Functional
public abstract class MorphTokenAnalysis {
  public abstract MorphologyAlgorithmDescription sourceAlgorithm();

  /**
   * The lemma should be thought of as the dictionary, or canonical form, of the token. This can
   * be used as a canonical form that removes inflectional morphology but generally leaves
   * derivational morphology intact. For example, for bakers the lemma would be baker.
   *
   * Similarly, for an inflected compound like doghouses, the lemma would be doghouse.
   * Leaving compounds intact except for inflectional endings is consistent with the method of
   * annotating compounds with morphological information, for example in the TIGER corpus
   * (Brants et al., 2004) of German.
   */
  public abstract ImmutableList<LexicalForm> lemmas();
  public abstract Optional<Double> score();

  /**
   * The roots can be thought of as the most fundamental signs in the token. This is meant to give
   * the deepest level of morphological analysis possible where inflectional and derivational
   * morphology is removed. For example, for bakers the root would be bake. The content of the
   * root field should reflect the concept of a lexeme in that it encodes the underlying abstract
   * sign that connects related forms. For many simple analyzers or stemmers, the root is not be
   * identified in the output, and the list of roots will be absent. For compounds, all roots
   * identified in the word should be given (for example, dog and house for doghouse).
   */
  public abstract ImmutableList<LexicalForm> roots();

  /**
   * Units of morphological significance. Typically the root is not included but this
   * is not guaranteed.
   */
  public abstract ImmutableList<Morph> morphs();

  /**
   * The morphosyntactic properties of the word (e.g. PLURAL)
   */
  public abstract ImmutableSet<MorphFeature> morphFeatures();

  public static class Builder extends ImmutableMorphTokenAnalysis.Builder {}

  @Value.Check
  protected void check() {
    checkArgument(lemmas().isEmpty() || sourceAlgorithm().providesLemmas(),
        "Source algorithm claims not to provide lemmas, but they are present");

    checkArgument(
        FluentIterable.from(lemmas()).transformAndConcat(LexicalFormFunctions.glosses()).isEmpty()
        || sourceAlgorithm().glossesLemmas(),
        "Source algorithm claims not to provide glosses, but the are present");
    checkArgument(
        FluentIterable.from(roots()).transformAndConcat(LexicalFormFunctions.glosses()).isEmpty()
            || sourceAlgorithm().glossesLemmas(),
        "Source algorithm claims not to provide glosses, but the are present");


    checkArgument(!score().isPresent() || sourceAlgorithm().providesTokenScores(),
      "Source algorithm claims not to provide token scores, but they are present");
    checkArgument(roots().isEmpty() || sourceAlgorithm().providesRoots(),
        "Source algorithm claims not to provide roots, but they are present");
    checkArgument(morphs().isEmpty() || sourceAlgorithm().providesMorphs(),
        "Source algorithm claims not to provide morphs, but they are present");

    checkArgument(morphFeatures().isEmpty() || sourceAlgorithm().providesFeatures(),
        "Source algorithm claims not to provide features, but they are present");
    checkArgument(
        FluentIterable.from(morphs()).transformAndConcat(MorphFunctions.features()).isEmpty()
            || sourceAlgorithm().providesFeatures(),
        "Source algorithm claims not to provide features, but the are present");
  }

  @Override
  public String toString() {
    final MoreObjects.ToStringHelper ret = MoreObjects.toStringHelper("");
    if (!lemmas().isEmpty()) {
      ret.add("lemmas", lemmas());
    }
    if (!morphFeatures().isEmpty()) {
      ret.add("features", morphFeatures());
    }
    if (!roots().isEmpty()) {
      ret.add("roots", roots());
    }
    if (!morphs().isEmpty()) {
      ret.add("morphs", morphs());
    }
    if (score().isPresent()) {
      ret.add("score", score());
    }
    ret.add("source", sourceAlgorithm().name());
    return ret.toString();
  }
}
