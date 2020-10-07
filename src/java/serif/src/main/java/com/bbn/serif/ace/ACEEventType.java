package com.bbn.serif.ace;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ACEEventType {

  public ACEEventType(String type, String subtype, List<ACEEventRole> roles) {
    this.type = checkNotNull(type);
    this.subtype = checkNotNull(subtype);
    this.roles = ImmutableList.copyOf(roles);
  }

  public String name() {
    return type + "." + subtype;
  }

  public Symbol symbol() {
    return Symbol.from(name());
  }

  public List<ACEEventRole> roles() {
    return roles;
  }

  public static final Function<ACEEventType, String> Name = new Function<ACEEventType, String>() {
    @Override
    public String apply(ACEEventType et) {
      return et.name();
    }
  };

  private final String type;
  private final String subtype;
  private final List<ACEEventRole> roles;
}
