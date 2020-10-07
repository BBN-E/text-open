package com.bbn.serif.constraints;

import com.bbn.serif.sentences.EachRegionToASentenceSegmenter;
import com.bbn.serif.sentences.SentenceFinder;

import com.google.common.annotations.Beta;
import com.google.inject.AbstractModule;

/**
 * Created by rgabbard on 3/18/16.
 */
@Beta
public final class ExtremelySimpleSentenceSegmenterM extends AbstractModule {

  @Override
  protected void configure() {
    bind(SentenceFinder.class).to(EachRegionToASentenceSegmenter.class);
  }
}
