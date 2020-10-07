package com.bbn.serif.relations;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.relations.constraints.RelationConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * This corresponds to CSerif's relations stage and adds SentenceTheory level relations to each
 * SentenceTheory in the DocTheory (if they exist). The implementation is required to verify that
 * the constraints are obeyed and warn or crash depending on tolerance.
 */
@Beta
public interface RelationFinder extends Finishable {

  DocTheory addRelations(final DocTheory docTheory, final Set<RelationConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.relations.jacserif.tolerant";
  }
}
