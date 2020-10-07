package com.bbn.serif.languages;

import com.bbn.nlp.languages.Vietnamese;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class VietnameseSerifLanguage extends SerifLanguage {

  @Inject
  private VietnameseSerifLanguage(@JsonProperty("language") Vietnamese lang) {
    super(lang);
  }


  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Vietnamese.Module());
    }
  }
}
