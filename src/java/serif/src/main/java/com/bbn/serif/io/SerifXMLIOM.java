package com.bbn.serif.io;

import com.bbn.serif.languages.SerifLanguage;

import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class SerifXMLIOM extends AbstractModule {

  @Provides
  public SerifXMLWriter getSerifXMLWriter() {
    return SerifXMLWriter.create();
  }

  @Provides
  public SerifXMLLoader getSerifXMLLoader(
      @AllowSloppyOffsetsP Boolean allowSloppyOffsets,
      @TranslateForNonZeroOriginalTextStartOffsetsP Boolean compatibilityWithDocumentsOffsetIntoSource,
      @SerifLanguage.ByLongNamesP SerifLanguage.SerifLanguageMap languageMap) {
    SerifXMLLoader.Builder ret = SerifXMLLoader.builder()
        .compatibilityWithDocumentsOffsetIntoSource(compatibilityWithDocumentsOffsetIntoSource)
        .languageLookupMap(languageMap);
    if (allowSloppyOffsets) {
      ret = ret.allowSloppyOffsets();
    }
    return ret.build();
  }

  @Override
  protected void configure() {
    bind(DocTheoryLoader.class).to(SerifXMLLoader.class);
  }

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface AllowSloppyOffsetsP {

  }

  /**
   * See {@link SerifXMLLoader} javadoc for details regarding 'compatibility with documents offset
   * into source'
   */
  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface TranslateForNonZeroOriginalTextStartOffsetsP {

  }
}
