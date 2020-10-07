package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.relations.RelationFinder;
import com.bbn.serif.relations.RelationMentionFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;


public class RelationsConstraintsM extends AbstractModule {

  private final Parameters params;

  public RelationsConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @RelationMentionFinder.TolerantP
  public boolean relationMentionConstraintTolerance(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(RelationMentionFinder.TolerantP.param).or(globalTolerance);
  }

  @Provides
  @RelationFinder.TolerantP
  public boolean relationConstraintTolerance(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(com.bbn.serif.relations.RelationFinder.TolerantP.param).or(globalTolerance);
  }
}
