package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class Genericity extends Attribute {

  public static Genericity SPECIFIC = new Genericity("Specific");
  public static Genericity GENERIC = new Genericity("Generic");
  private static final ImmutableList<Genericity> VALUES = ImmutableList.of(SPECIFIC, GENERIC);

  public static Genericity from(Symbol s) {
    return parse(s, stringToGenericity, "Genericity");
  }

  private Genericity(String name) {
    super(name);
  }

  private static final ImmutableMap<Symbol, Genericity> stringToGenericity =
      Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
