package com.bbn.serif.entities;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.entities.constraints.EntityConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Augments a DocTheory with Entities. Corresponds to Serif's entities stage.
 *
 * Each implementation is required to enforce tolerance to constraint satisfaction.
 */
@Beta
public interface EntityFinder extends Finishable {

  DocTheory addEntities(final DocTheory input, final Set<EntityConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {
    String param = "com.bbn.serif.entities.jacserif.tolerant";
  }
}
