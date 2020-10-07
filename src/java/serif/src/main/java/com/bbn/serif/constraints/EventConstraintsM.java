package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.constraints.ConstrainedSerif;
import com.bbn.serif.events.EventFinder;
import com.bbn.serif.events.EventMentionFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public final class EventConstraintsM extends AbstractModule {

  private final Parameters params;

  public EventConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @EventFinder.TolerantP
  public boolean eventConstraintViolationTolerant(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(EventFinder.TolerantP.param).or(globalTolerance);
  }

  @Provides
  @EventMentionFinder.TolerantP
  public boolean eventMentionConstraintViolationTolerant(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(EventMentionFinder.TolerantP.param).or(globalTolerance);
  }
}
