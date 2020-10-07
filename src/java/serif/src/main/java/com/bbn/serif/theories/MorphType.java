package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * The type of a {@link Morph} (e.g. "prefix"). These are strongly interned, so a program
 * should not use an unbounded set of them.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable(intern = true)
@JsonSerialize
@JsonDeserialize
public abstract class MorphType {
  public abstract Symbol name();

  public static MorphType of(String name) {
    return new Builder().name(Symbol.from(name)).build();
  }

  public static class Builder extends ImmutableMorphType.Builder {}
}
