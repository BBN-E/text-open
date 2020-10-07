package com.bbn.serif.languages;

import com.bbn.nlp.languages.Oromo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class OromoSerifLanguage extends SerifLanguage {
  @Inject
  private OromoSerifLanguage(@JsonProperty("language") Oromo uyghur) {
    super(uyghur);
  }

  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Oromo.Module());
    }
  }
}
