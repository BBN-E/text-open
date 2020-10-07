package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public final class GainLoss extends Attribute {

  public static GainLoss GAIN = new GainLoss("GAIN");
  public static GainLoss LOSS = new GainLoss("LOSS");
  private static final ImmutableList<GainLoss> VALUES = ImmutableList.of(GAIN, LOSS);

  public static GainLoss from(Symbol s) {
    return parse(s, stringToGainLoss, "GainLoss");

  }

  private GainLoss(String name) {
    super(name);
  }

  private static final ImmutableMap<Symbol, GainLoss> stringToGainLoss =
      Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
