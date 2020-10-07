package com.bbn.serif.events;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.events.constraints.EventMentionConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Adds EventMentions to each SentenceTheory within a DocTheory. Each implementation is responsible
 * for enforcing constraint satisfaction at the correct tolerance level.
 */

@Beta
public interface EventMentionFinder extends Finishable {

  DocTheory addEventMentions(final DocTheory input,
      final Set<EventMentionConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.events.mentions.jacserif.tolerant";
  }
}
