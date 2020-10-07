package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.types.EntityType;

import com.google.common.base.Optional;

import org.immutables.func.Functional;
import org.immutables.value.Value;

/**
 * A named entity in a Serif document.
 */
@Value.Immutable
@TextGroupImmutable
@Functional
public abstract class Name implements Spanning, HasExternalID, WithName {

  /**
   * Creates a {@code Name} of the specified {@code type} over the specified {@code span}.
   */
  public static Name of(EntityType type, TokenSequence.Span span) {
    return ImmutableName.of(type, span);
  }

  @Value.Parameter
  public abstract EntityType type();

  @Override
  @Value.Parameter
  public abstract TokenSequence.Span span();

  public abstract Optional<Double> score();

  public abstract Optional<String> transliteration();

  @Override
  public abstract Optional<Symbol> externalID();

  public static Name.Builder builder(TokenSequence.Span span, EntityType type) {
    return new Name.Builder().type(type).span(span);
  }

  @Override
  public final TokenSpan tokenSpan() {
    return span();
  }

  public static class Builder extends ImmutableName.Builder {}

  @Override
  public String toString() {
    String ret = type().name().asString() + ":" + span().tokenizedText();
    if (transliteration().isPresent()) {
      ret += " --> " + transliteration().get();
    }
    if (externalID().isPresent()) {
      ret += "; externaLId=" + externalID().get();
    }
    if (score().isPresent()) {
      ret += "=" + String.format("%.3f", score().get());
    }
    return ret;
  }

}
