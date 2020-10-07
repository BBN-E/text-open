package com.bbn.serif.languages;

import com.bbn.nlp.languages.Yoruba;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class YorubaSerifLanguage extends SerifLanguage {

  @Inject
  private YorubaSerifLanguage(@JsonProperty("language") Yoruba lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Yoruba.Module());
    }
  }
}
