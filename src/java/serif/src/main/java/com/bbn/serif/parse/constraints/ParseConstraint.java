package com.bbn.serif.parse.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

/**
 * Unimplemented ParseConstraints
 */
@Beta
public interface ParseConstraint {

  boolean satisfiedBy(final DocTheory dt);

}
