package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;

import java.util.Map;

/**
 * Indicates this object can store arbitrary metadata.  Metadata is useful for debugging and
 * evaluation-specific hacks, but it should not be used as an alternative to more structure
 * solutions for long-term use.
 */
public interface HasMetadata {

  Map<Symbol, String> metadata();
}
