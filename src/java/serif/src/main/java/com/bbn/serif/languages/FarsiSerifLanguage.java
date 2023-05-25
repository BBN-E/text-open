package com.bbn.serif.languages;

import com.bbn.bue.common.SerifLocale;
import com.bbn.bue.common.symbols.Symbol;

import com.bbn.nlp.languages.Language;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

public final class FarsiSerifLanguage extends SerifLanguage {

  public static class FarsiFinal extends Language {

    private static final String LONG_NAME = "Farsi";
    private static final Symbol ISO_693_1_CODE = Symbol.from("fa");
    private static final Symbol ISO_693_2_CODE = Symbol.from("fas");
    private static final SerifLocale DEFAULT_LOCALE = SerifLocale.forLocaleString("fa");

    protected FarsiFinal() {
      super(ISO_693_1_CODE.asString(), LONG_NAME);
    }

    @Override
    public String longName() {
      return LONG_NAME;
    }

    @Override
    public Symbol iso639Dash1Code() {
      return ISO_693_1_CODE;
    }

    @Override
    public Symbol iso639Dash2Code() {
      return ISO_693_2_CODE;
    }

    @Override
    public SerifLocale defaultLocale() {
      return DEFAULT_LOCALE;
    }

    public static class Module extends AbstractModule {

      @Override
      protected void configure() {
      }
    }
  }

  @Inject
  private FarsiSerifLanguage(@JsonProperty("language") FarsiFinal lang) {
    super(lang);
  }

  private static final FarsiSerifLanguage INSTANCE = new FarsiSerifLanguage(new FarsiFinal());

  public static FarsiSerifLanguage getInstance() {
    return INSTANCE;
  }

  public static class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new FarsiFinal.Module());
    }
  }
}
