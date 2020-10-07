package com.bbn.serif.constraints;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.parse.Parser;
import com.bbn.serif.parse.FlatTreeParserFromOffsets;

import com.google.common.annotations.Beta;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Created by rgabbard on 3/18/16.
 */
@Beta
public final class FlatTreeParserFromOffsetsM extends AbstractModule {

  @Override
  protected void configure() {
    bind(Parser.class).to(FlatTreeParserFromOffsets.class);
  }

  @Provides
  @Parser.TolerantP
  public boolean tolerant(final Parameters params) {
    return params.getOptionalBoolean(Parser.TolerantP.param)
        .or(params.getBoolean(ConstrainedSerif.GlobalTolerance.param));
  }
}
