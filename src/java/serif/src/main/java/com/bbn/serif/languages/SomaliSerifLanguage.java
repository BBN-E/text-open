package com.bbn.serif.languages;

import com.bbn.nlp.languages.Somali;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class SomaliSerifLanguage extends SerifLanguage {

  @Inject
  private SomaliSerifLanguage(@JsonProperty("language") Somali lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Somali.Module());
    }
  }
}
