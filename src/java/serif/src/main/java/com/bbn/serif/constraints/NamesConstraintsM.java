package com.bbn.serif.constraints;


import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.names.NameFinder;

import com.google.common.annotations.Beta;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

@Beta
public final class NamesConstraintsM extends AbstractModule {

  private final Parameters params;

  public NamesConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @NameFinder.TolerantP
  public boolean nameConstraintViolationTolerance(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(NameFinder.TolerantP.param).or(globalTolerance);
  }
}
