package com.bbn.serif.types;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The ontology type of a {@link com.bbn.serif.theories.Value} or
 * {@link com.bbn.serif.theories.ValueMention}.
 *
 * For historical reasons, unlike the separation of levels of type seen in {@link EntityType}
 * and {@link EntitySubtype}, each  {@link ValueType} is split into two parts, the
 * {@link #baseTypeSymbol()} and {@link #subtypeSymbol()}, The identity of a value type for purposes
 * of equality and hash codes is determined by both of these together.  For convenience, a method
 * {@link #name()} is provided which will join the {@link #baseTypeSymbol()} and
 * {@link #subtypeSymbol()} with a {@code .} according to CSerif's convention.  Also for historical
 * reasons neither component may itself contain a period.
 *
 * CSerif uses the special type {@code UNDET} to indicate a value type has not been determined.
 * For convenience, {@link #isDetermined()} is provided to check for this.
 *
 * Identity for equality and hashcode is determined by the base type and subtype together.
 */
@TextGroupImmutable
// we used to use intern=true, but it led to
// initialization problems
@Value.Immutable
@JsonSerialize(as = ImmutableValueType.class)
@JsonDeserialize(as = ImmutableValueType.class)
public abstract class ValueType {

  public abstract Symbol baseTypeSymbol();

  public abstract Optional<Symbol> subtypeSymbol();

  /**
   * The full name of an value type in the Serif style.  This is the {@link #baseTypeSymbol()}
   * and {@link #subtypeSymbol()} joined by a {@code .} if the subtype symbol is present and is
   * the {@link #baseTypeSymbol()} alone otherwise.
   */
  @Value.Derived
  public Symbol name() {
    if (subtypeSymbol().isPresent()) {
      return Symbol.from(baseTypeSymbol().asString() + "." + subtypeSymbol().get().asString());
    } else {
      return baseTypeSymbol();
    }
  }

  @Value.Check
  protected void check() {
    checkArgument(!baseTypeSymbol().asString().contains("."),
        "Value base types may not contain periods");
    if (subtypeSymbol().isPresent()) {
      checkArgument(!subtypeSymbol().get().asString().contains("."),
          "Value sub types may not contain periods");
    }
  }

  /**
   * Parses a string for the form {@code baseType.subType} to a {@link ValueType}. If no
   * period is present, the full string is used as the base type and there is no subtype.
   */
  public static ValueType parseDottedPair(final String s) {
    final int dotPosition = s.lastIndexOf(".");
    if (dotPosition >= 0 && (dotPosition + 1) < s.length()) {
      return ImmutableValueType.builder()
          .baseTypeSymbol(Symbol.from(s.substring(0, dotPosition)))
          .subtypeSymbol(Symbol.from(s.substring(dotPosition + 1)))
          .build();
    } else {
      return ImmutableValueType.builder().baseTypeSymbol(Symbol.from(s)).build();
    }
  }

  private enum DottedPairParser implements Function<String ,ValueType> {
    INSTANCE;

    @Override
    public ValueType apply(final String input) {
      return parseDottedPair(input);
    }
  }

  public static Function<String, ValueType> fromDottedPairFunction() {
    return DottedPairParser.INSTANCE;
  }

  private static final Symbol UNDET = Symbol.from("UNDET");

  /**
   * Checks for any value type with base type {@code UNDET}.
   */
  public final boolean isDetermined() {
    return !baseTypeSymbol().equalTo(UNDET);
  }

  @Override
  public String toString() {
    return name().asString();
  }
}
