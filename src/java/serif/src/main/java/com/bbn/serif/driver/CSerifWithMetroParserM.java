package com.bbn.serif.driver;

// this code appears to be incomplete and has problematic dependencies, so I'm commenting it out.
/*
import com.bbn.serif.CSerifProcessingStep;
import com.bbn.serif.CSerifStage;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.List;

public final class CSerifWithMetroParserM extends AbstractModule {

  @Provides
  public List<ProcessingStep> getProcessingSteps(CSerifProcessingStep.Factory stepFactory) {
    ImmutableList.Builder builder = ImmutableList.builder();
    builder.add(stepFactory.make(CSerifStage.START, CSerifStage.VALUES));
    // TODO: Insert call to Metro parser here
    builder.add(stepFactory.make(CSerifStage.PARSE, CSerifStage.PARSE));
    builder.add(stepFactory.make(CSerifStage.MENTIONS, CSerifStage.OUTPUT));
    return builder.build();
  }

  @Override
  protected void configure() {
  }
}
*/
