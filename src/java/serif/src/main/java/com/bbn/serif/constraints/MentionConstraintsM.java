package com.bbn.serif.constraints;


import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.constraints.ConstrainedSerif;
import com.bbn.serif.mentions.MentionFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public final class MentionConstraintsM extends AbstractModule {

  private final Parameters params;

  public MentionConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @MentionFinder.TolerantP
  public boolean mentionConstraintViolationTolerance(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(MentionFinder.TolerantP.param).or(globalTolerance);
  }
}
