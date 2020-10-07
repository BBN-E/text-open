package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a morphsyntactic property (e.g plural, passive voice, etc.). This class is
 * strongly interned, so the set of {@link MorphFeature}s a program works with should not be
 * unbounded.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable(intern=true)
@Functional
@JsonSerialize
@JsonDeserialize
public abstract class MorphFeature {
  public abstract Symbol name();

  public static MorphFeature of(String name) {
    return new Builder().name(Symbol.from(name)).build();
  }

  @Value.Check
  protected void check() {
    checkArgument(!name().asString().contains(","), "For serialization convenience, "
        + "MorphFeatures are forbidden from containing commas");
  }

  @Override
  public String toString() {
    return name().asString();
  }

  public static class Builder extends ImmutableMorphFeature.Builder {}
}
