package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class Modality extends Attribute {

  public static Modality ASSERTED = new Modality("Asserted");
  public static Modality OTHER = new Modality("Other");
  private static final ImmutableList<Modality> VALUES = ImmutableList.of(ASSERTED, OTHER);

  public static Modality from(Symbol s) {
    return parse(s, stringToModality, "Modality");
  }

  private Modality(String name) {
    super(name);
  }

  private static final ImmutableMap<Symbol, Modality> stringToModality =
      Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
