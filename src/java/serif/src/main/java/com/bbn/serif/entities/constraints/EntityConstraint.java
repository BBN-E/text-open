package com.bbn.serif.entities.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

/**
 * EntityConstraints - unimplemented
 */
@Beta
public interface EntityConstraint {

  boolean satisfiedBy(final DocTheory dt);
}
