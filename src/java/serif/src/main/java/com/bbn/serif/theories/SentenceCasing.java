package com.bbn.serif.theories;

import com.bbn.bue.common.LetterCounts;
import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

/**
 * Summarizes the casing observed in a sentence.
 */
@JsonSerialize
@JsonDeserialize
@Value.Immutable
@TextGroupImmutable
public abstract class SentenceCasing {

  /**
   * The {@link LetterCounts} of all code points in a sentence's tokens.
   */
  @Value.Parameter
  public abstract LetterCounts forAllCharacters();

  /**
   * The {@link LetterCounts} of all token-initial code points in a sentence's tokens.
   */
  @Value.Parameter
  public abstract LetterCounts tokenInitial();

  public static class Builder extends ImmutableSentenceCasing.Builder {}
}

