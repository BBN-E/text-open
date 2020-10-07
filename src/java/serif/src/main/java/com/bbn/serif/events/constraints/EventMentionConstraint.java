package com.bbn.serif.events.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

@Beta
public interface EventMentionConstraint {

  boolean satisfiedBy(final DocTheory dt);
}
