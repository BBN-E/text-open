package com.bbn.serif.constraints;


import com.bbn.bue.common.symbols.Symbol;

import com.google.common.annotations.Beta;

/**
 * Provides constraints derived from the given annotation for Constrained Serif stages.
 */
@Beta
public interface ConstraintSource {

  SerifConstraints constraintsForDocument(final Symbol docID);

}
