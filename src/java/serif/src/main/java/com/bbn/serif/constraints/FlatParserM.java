package com.bbn.serif.constraints;

import com.bbn.serif.parse.Parser;
import com.bbn.serif.parse.FlatParser;

import com.google.common.annotations.Beta;
import com.google.inject.AbstractModule;

/**
 * Created by rgabbard on 3/18/16.
 */
@Beta
public final class FlatParserM extends AbstractModule {

  @Override
  protected void configure() {
    bind(Parser.class).to(FlatParser.class);
  }
}
