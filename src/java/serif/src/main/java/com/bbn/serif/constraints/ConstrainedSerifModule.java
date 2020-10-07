package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;

import com.google.common.annotations.Beta;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Created by rgabbard on 3/18/16.
 */
@Beta
public final class ConstrainedSerifModule extends AbstractModule {

  private final Parameters params;

  public ConstrainedSerifModule(final Parameters params) {
    this.params = params;
  }

  @Override
  protected void configure() {
    install(new TokenConstraintsM(params));
    install(new NamesConstraintsM(params));
    install(new ValueConstraintsM(params));
    install(new MentionConstraintsM(params));
    install(new EntityConstraintsM(params));
    install(new RelationsConstraintsM(params));
    install(new EventConstraintsM(params));
    // TODO: in the future, use this as an entry point.
  }

  @Provides
  @ConstrainedSerif.GlobalTolerance
  public boolean globalTolerance() {
    return params.getBoolean(ConstrainedSerif.GlobalTolerance.param);
  }

  @Provides
  @ConstrainedSerif.CharsToSkipP
  public int charsToSkip() {
    return params.getOptionalInteger(ConstrainedSerif.CharsToSkipP.param).or(0);
  }
}
