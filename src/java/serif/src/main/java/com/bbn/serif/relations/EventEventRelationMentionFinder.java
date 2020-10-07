package com.bbn.serif.relations;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.relations.constraints.EventEventRelationMentionConstraint;
import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Adds EventEventRelationMentions subject to certain constraints. The implementation is required to verify
 * that the constraints are obeyed.
 */
@Beta
public interface EventEventRelationMentionFinder extends Finishable {

  DocTheory addEventEventRelationMentions(final DocTheory input,
                                final Set<EventEventRelationMentionConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.relations.mentions.jacserif.tolerant";
  }
}
