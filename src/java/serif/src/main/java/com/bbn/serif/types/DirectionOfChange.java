package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;


public final class DirectionOfChange extends Attribute {

    public static DirectionOfChange INCREASE = new DirectionOfChange("Increase");
    public static DirectionOfChange DECREASE = new DirectionOfChange("Decrease");
    public static DirectionOfChange UNSPECIFIED = new DirectionOfChange("Unspecified");

    private static final ImmutableList<DirectionOfChange> VALUES = ImmutableList.of(INCREASE, DECREASE, UNSPECIFIED);

    public static DirectionOfChange from(Symbol s) {
        return parse(s, stringToDirectionOfChange, "DirectionOfChange");
    }

    private DirectionOfChange(String name) {
        super(name);
    }

    private static final ImmutableMap<Symbol, DirectionOfChange> stringToDirectionOfChange =
            Maps.uniqueIndex(VALUES, Attribute.nameFunction());
}
