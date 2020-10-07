package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class Trend extends Attribute{
    public static Trend INCREASE = new Trend("Increase");
    public static Trend DECREASE = new Trend("Decrease");
    public static Trend STABLE = new Trend("Stable");
    public static Trend UNSPECIFIED = new Trend("Unspecified");

    private static final ImmutableList<Trend> VALUES = ImmutableList.of(INCREASE, DECREASE, STABLE,UNSPECIFIED);

    public static Trend from(Symbol s) {
        return parse(s, stringToTrend, "Trend");
    }

    private static final ImmutableMap<Symbol, Trend> stringToTrend =
            Maps.uniqueIndex(VALUES, Attribute.nameFunction());
    protected Trend(String name) {
        super(name);
    }
}
