package com.bbn.serif.languages;

import com.bbn.nlp.languages.Russian;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class RussianSerifLanguage extends SerifLanguage {

  @Inject
  private RussianSerifLanguage(@JsonProperty("language") Russian lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Russian.Module());
    }
  }
}
