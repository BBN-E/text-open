package com.bbn.serif;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;

/**
 * A strategy for mapping documents to some equivalence classes.  Typically this is used to capture
 * e.g. genre distinctions.
 */
public interface DocTypeMapper {

  Symbol mapSourceType(final DocTheory dt);
}
