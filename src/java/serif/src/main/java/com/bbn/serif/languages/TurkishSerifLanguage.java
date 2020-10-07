package com.bbn.serif.languages;

import com.bbn.nlp.languages.Turkish;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class TurkishSerifLanguage extends SerifLanguage {
  @Inject
  private TurkishSerifLanguage(@JsonProperty("language") Turkish turkish) {
    super(turkish);
  }

  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Turkish.Module());
    }
  }
}

