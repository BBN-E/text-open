package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.entities.EntityFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Created by jdeyoung on 3/1/16.
 */
public class EntityConstraintsM extends AbstractModule {

  private final Parameters params;

  public EntityConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @EntityFinder.TolerantP
  public boolean tolerance(@ConstrainedSerif.GlobalTolerance boolean globalTolerance) {
    return params.getOptionalBoolean(EntityFinder.TolerantP.param).or(globalTolerance);
  }

  // TODO entity implementations
}
