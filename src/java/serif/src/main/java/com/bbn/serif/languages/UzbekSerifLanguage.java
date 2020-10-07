package com.bbn.serif.languages;

import com.bbn.nlp.languages.Uzbek;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class UzbekSerifLanguage extends SerifLanguage {
  @Inject
  private UzbekSerifLanguage(@JsonProperty("language") Uzbek uzbek) {
    super(uzbek);
  }

  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Uzbek.Module());
    }
  }
}
