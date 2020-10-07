package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Optional;

/**
 * Indicates that the given theory bears an ID which corresponds to some external object. For
 * example, we use this to trakc the correspondence of Serif objects to ERE annotation objects.
 */
public interface HasExternalID {

  Optional<Symbol> externalID();
}
