package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class Tense extends Attribute {

  public static Tense UNSPECIFIED = new Tense("Unspecified");
  public static Tense PAST = new Tense("Past");
  public static Tense PRESENT = new Tense("Present");
  public static Tense FUTURE = new Tense("Future");

  private static final ImmutableList<Tense> VALUES =
      ImmutableList.of(UNSPECIFIED, PAST, PRESENT, FUTURE);

  public static Tense from(Symbol s) {
    return parse(s, stringToTense, "Tense");
  }

  private Tense(String name) {
    super(name);
  }

  private static final ImmutableMap<Symbol, Tense> stringToTense =
      Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
