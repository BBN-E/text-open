package com.bbn.serif.io;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import java.util.Collection;

/**
 * A way of converting between symbols and types. For example, between entity type symbols like PER
 * and GPE and their corresponding entity types.
 */
public interface TypeSource<T> {

  Optional<T> fromSymbol(Symbol typeSymbol);

  T fromSymbolRequired(Symbol typeSymbol);

  Function<Symbol, T> asFunction();

  /**
   * Get all types currently known.
   */
  Collection<T> currentlyKnown();
}
