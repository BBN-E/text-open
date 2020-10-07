package com.bbn.serif.names.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

/**
 * NameConstraints - unimplemented
 */
@Beta
public interface NameConstraint {

  boolean satisfiedBy(final DocTheory dt);
}
