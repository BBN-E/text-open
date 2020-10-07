package com.bbn.serif.languages;

import com.bbn.nlp.languages.Hausa;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class HausaSerifLanguage extends SerifLanguage {
  @Inject
  private HausaSerifLanguage(@JsonProperty("language") Hausa hausa) {
    super(hausa);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Hausa.Module());
    }
  }
}
