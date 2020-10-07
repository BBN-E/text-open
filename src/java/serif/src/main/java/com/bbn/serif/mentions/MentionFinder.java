package com.bbn.serif.mentions;

import com.bbn.bue.common.Finishable;
import com.bbn.serif.mentions.constraints.MentionConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Adds mentions to a DocTheory. This corresponds to CSerif's Mentions stage. Each Adder is required
 * to verify that constraints are satisfied and if not, respond appropriately according to the
 * tolerance to errors.
 */
@Beta
public interface MentionFinder extends Finishable {

  DocTheory addMentions(final DocTheory input, final Set<MentionConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.mentions.jacserif.tolerant";
  }
}
