package com.bbn.serif.languages;


import com.bbn.nlp.languages.Arabic;
import com.bbn.serif.theories.SynNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;


public class ArabicSerifLanguage extends SerifLanguage {
  @Inject
  private ArabicSerifLanguage(@JsonProperty("language") Arabic arabic) {
    super(arabic);
  }

  private static final ArabicSerifLanguage INSTANCE = new ArabicSerifLanguage(Arabic.getInstance());

  public static ArabicSerifLanguage getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean isVerbExcludingModals(SynNode node) {
    return language().isVerbalPOSExcludingModals(node.tag());
  }


  public static final class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Arabic.Module());
    }
  }

}

