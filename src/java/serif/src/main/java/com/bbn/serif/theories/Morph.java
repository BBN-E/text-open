package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;

/**
 * A sign bearing morphlogical information.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@JsonSerialize
@JsonDeserialize
@Functional
public abstract class Morph {

  /**
   * The form of this sign.  It may correspond to its manifestation in the surface string
   * exactly or may be something more absract.
   */
  public abstract Symbol form();
  // offsets relative to start of token
  //public abstract List<OffsetRange<CharOffset>> exponentTokenRelativeOffsets();

  /**
   * The morphosyntactic properties carried by this {@link Morph} (e.g. plural)
   */
  public abstract ImmutableSet<MorphFeature> features();

  /**
   * The type of morphological marker this is (e.g. prefix)
   */
  public abstract MorphType morphType();

  public static class Builder extends ImmutableMorph.Builder {}

}
