package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Describes the capability of an algorithms used to generate morphological analyses in this
 * document.  Multiple algorithms (e.g. a rule-based and an unsupervised system) can be
 * applied to the same document.  This object will be referred to be {@link MorphTokenSequence}s
 * and {@link MorphTokenAnalysis}es.
 *
 * We use an object like this rather than using {@link com.google.common.base.Optional} throughout
 * the morphology theories. If an algorithm needs to distinguish between e.g. a token which has
 * has been analyzed no morphological features and one which lacks such features because the
 * analyzer doesn't produce them, it should consult this object.
 *
 * Typically when generating morphology information for a document you only want to generate
 * one of these descriptions and reuse it.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@JsonSerialize
@JsonDeserialize
public abstract class MorphologyAlgorithmDescription {
  public abstract Symbol name();

  @Value.Default
  public boolean providesMorphs() {
    return false;
  }

  @Value.Default
  public boolean providesFeatures() {
    return false;
  }

  @Value.Default
  public boolean providesLemmas() {
    return false;
  }

  @Value.Default
  public boolean providesRoots() {
    return false;
  }

  @Value.Default
  public boolean providesSequenceScores() {
    return false;
  }

  @Value.Default
  public boolean providesTokenScores() {
    return false;
  }

  @Value.Default
  public boolean glossesLemmas() {
    return false;
  }

  @Value.Default
  public boolean glossesRoots() {
    return false;
  }

  @Value.Default
  public boolean hasLexicon() {
    return false;
  }

  @Value.Check
  protected void check() {
    checkArgument(providesLemmas() || !glossesLemmas(),
        "Can't gloss lemmas you don't provide");
    checkArgument(providesRoots() || !glossesRoots(),
        "Can't gloss roots you don't provide");
  }

  public static class Builder extends ImmutableMorphologyAlgorithmDescription.Builder {}
}

