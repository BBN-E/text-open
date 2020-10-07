package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.func.Functional;

/**
 * Represents the meaning of a {@link LexicalForm}
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@Functional
@JsonSerialize
@JsonDeserialize
public abstract class Gloss {
  public abstract String gloss();

  public static Gloss of(String gloss) {
    return new Builder().gloss(gloss).build();
  }

  public static class Builder extends ImmutableGloss.Builder {}
}
