package com.bbn.serif.relations.constraints;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

@Beta
public interface RelationMentionConstraint {

  boolean satisfiedBy(final DocTheory dt);
}
