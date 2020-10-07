package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.values.ValueMentionFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ValueConstraintsM extends AbstractModule {

  private final Parameters params;

  public ValueConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @ValueMentionFinder.TolerantP
  public boolean valueConstraintTolerance(@ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(ValueMentionFinder.TolerantP.param).or(globalTolerance);
  }
}
