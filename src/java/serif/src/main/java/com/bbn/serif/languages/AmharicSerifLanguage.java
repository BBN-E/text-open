package com.bbn.serif.languages;

import com.bbn.nlp.languages.Amharic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class AmharicSerifLanguage extends SerifLanguage {

  @Inject
  private AmharicSerifLanguage(@JsonProperty("language") Amharic lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Amharic.Module());
    }
  }
}
