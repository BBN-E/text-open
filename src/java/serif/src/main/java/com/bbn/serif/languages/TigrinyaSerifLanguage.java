package com.bbn.serif.languages;

import com.bbn.nlp.languages.Tigrinya;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class TigrinyaSerifLanguage extends SerifLanguage {
  @Inject
  private TigrinyaSerifLanguage(@JsonProperty("language") Tigrinya uyghur) {
    super(uyghur);
  }

  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Tigrinya.Module());
    }
  }
}
