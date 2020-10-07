package com.bbn.serif.mentions.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

/**
 * MentionConstraints - unimplemented
 */
@Beta
public interface MentionConstraint {

  boolean satisfiedBy(final DocTheory dt);
}
