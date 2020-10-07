package com.bbn.serif.events.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

@Beta
public interface EventConstraint {

  boolean satisfiedBy(final DocTheory dt);
}
