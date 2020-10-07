package com.bbn.serif.parse;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.parse.constraints.ParseConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Adds a Parse to a SentenceTheory. This corresponds to CSerif's Parse stage. Each implementation
 * is responsible for verifying that constraints are satisfied and either throwing an exception or
 * warning the user as appropriate.
 */
@Beta
public interface Parser extends Finishable {

  DocTheory addParse(final DocTheory input, final Set<ParseConstraint> parseConstraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.parse.jacserif.tolerant";
  }
}
