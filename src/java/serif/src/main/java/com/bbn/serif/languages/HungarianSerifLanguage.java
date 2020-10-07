package com.bbn.serif.languages;

import com.bbn.nlp.languages.Hungarian;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class HungarianSerifLanguage extends SerifLanguage {

  @Inject
  private HungarianSerifLanguage(@JsonProperty("language") Hungarian lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Hungarian.Module());
    }
  }
}
