package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class Indicator extends Attribute {

  public static Indicator UNKNOWN = new Indicator("Unknown");
  public static Indicator SUCCESS = new Indicator("Success");
  public static Indicator FAILURE = new Indicator("Failure");
  private static final ImmutableList<Indicator> VALUES =
      ImmutableList.of(UNKNOWN, SUCCESS, FAILURE);

  public static Indicator from(Symbol s) {
    return parse(s, stringToIndicator, "Indicator");
  }

  private Indicator(String name) {
    super(name);
  }

  private static final ImmutableMap<Symbol, Indicator> stringToIndicator =
      Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
