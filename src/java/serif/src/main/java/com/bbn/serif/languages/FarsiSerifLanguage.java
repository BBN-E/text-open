package com.bbn.serif.languages;

import com.bbn.nlp.languages.Farsi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class FarsiSerifLanguage extends SerifLanguage {

  @Inject
  private FarsiSerifLanguage(@JsonProperty("language") Farsi lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Farsi.Module());
    }
  }
}
