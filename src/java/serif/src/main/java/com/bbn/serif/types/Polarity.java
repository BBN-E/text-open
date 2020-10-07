package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class Polarity extends Attribute {

  public static Polarity POSITIVE = new Polarity("Positive");
  public static Polarity NEGATIVE = new Polarity("Negative");
  private static final ImmutableList<Polarity> VALUES = ImmutableList.of(POSITIVE, NEGATIVE);

  public static Polarity from(Symbol s) {
    return parse(s, stringToPolarity, "Polarity");
  }

  private Polarity(String name) {
    super(name);
  }

  private static final ImmutableMap<Symbol, Polarity> stringToPolarity =
      Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
