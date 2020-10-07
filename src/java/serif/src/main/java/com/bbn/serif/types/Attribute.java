package com.bbn.serif.types;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Attribute {

  public Symbol name() {
    return name;
  }

  protected Attribute(String name) {
    this.name = Symbol.from(name);
  }

  @Override
  public String toString() {
    return this.name.toString();
  }

  protected static <T> T parse(Symbol s, Map<Symbol, T> valuesMap, String typeName) {
    final T ret = valuesMap.get(checkNotNull(s));
    if (ret != null) {
      return ret;
    } else {
      throw new RuntimeException(String.format("Unknown %s %s", typeName, s));
    }
  }

  private final Symbol name;

  /**
   * Prefer {@link #nameFunction()}
   */
  @Deprecated
  public static final Function<Attribute, Symbol> Name =
      new Function<Attribute, Symbol>() {
        @Override
        public Symbol apply(Attribute a) {
          return a.name();
        }
      };

  public static final Function<Attribute, Symbol> nameFunction() {
    return NameFunction.INSTANCE;
  }

  private enum NameFunction implements Function<Attribute, Symbol> {
    INSTANCE;

    @Override
    public Symbol apply(final Attribute input) {
      return input.name;
    }
  }

  public int stableHashCode() {
    return name.stableHashCode();
  }
}
