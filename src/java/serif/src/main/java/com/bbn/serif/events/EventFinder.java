package com.bbn.serif.events;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.events.constraints.EventConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * This corresponds to CSerif's events stage and adds sentence level events
 */
@Beta
public interface EventFinder extends Finishable {

  DocTheory addEvents(final DocTheory input, final Set<EventConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {
    String param = "com.bbn.serif.events.jacserif.tolerant";
  }
}
