package com.bbn.serif.values;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.values.constraints.ValueMentionConstraint;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Augments a DocTheory with ValueMentions. Corresponds to CSerif's Values stage. The implementation is
 * responsible for verifying that the constraints are satisfied.
 */
@Beta
public interface ValueMentionFinder extends Finishable {

  DocTheory addValues(final DocTheory input, final Set<ValueMentionConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {
    String param = "com.bbn.serif.values.jacserif.tolerant";
  }
}
