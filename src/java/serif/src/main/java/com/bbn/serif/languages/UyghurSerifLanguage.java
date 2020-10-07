package com.bbn.serif.languages;

import com.bbn.nlp.languages.Uyghur;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class UyghurSerifLanguage extends SerifLanguage {
  @Inject
  private UyghurSerifLanguage(@JsonProperty("language") Uyghur uyghur) {
    super(uyghur);
  }

  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Uyghur.Module());
    }
  }
}
