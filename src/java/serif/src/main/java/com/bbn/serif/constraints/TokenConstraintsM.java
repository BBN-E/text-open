package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.tokens.TokenFinder;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;


public class TokenConstraintsM extends AbstractModule {

  private final Parameters params;

  public TokenConstraintsM(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {

  }

  @Provides
  @TokenFinder.TolerantP
  public boolean tokenizerConstraintTolerance(
      @ConstrainedSerif.GlobalTolerance final boolean globalTolerance) {
    return params.getOptionalBoolean(TokenFinder.TolerantP.param).or(globalTolerance);
  }
}
