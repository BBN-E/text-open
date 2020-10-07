package com.bbn.serif;

import com.bbn.bue.common.ModuleFromParameter;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.serialization.jackson.BUECommonModule;
import com.bbn.bue.common.serialization.jackson.JacksonSerializationM;
import com.bbn.bue.learning2.LearningM;
import com.bbn.nlp.SerifLocaleM;
import com.bbn.serif.driver.ProcessingStep;
import com.bbn.serif.io.SerifXMLIOFromParamsM;
import com.bbn.serif.languages.EnglishSerifLanguage;
import com.bbn.serif.languages.SerifLanguage;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A module to load some frequently used parts of the Serif environment. Almost all JSerif programs
 * should install this module.
 *
 * Currently, this sets up:
 *
 * <ul>
 *   <li>The primary language for JSerif. This will be available in all code bound to
 *   {@link com.bbn.serif.languages.SerifLanguage}. The default is {@link EnglishSerifLanguage.Module}
 *   but you may specify something else.</li>
 *
 *   <li>SerifXML reading and writing. To find the parameters for this, see
 *   {@link SerifXMLIOFromParamsM}.</li>
 *
 *   <li>You can bind pre- and post-processors (which an application may or may not use) with
 * {@code com.bbn.serif.preprocessorModule} and {@code com.bbn.serif.postprocessorModules}.
 * Beware that the order of pre- and post-processors is guaranteed only if they are all installed in
 * a single module!</li>
 *
 * </ul>
 * */
public class SerifEnvironmentM extends AbstractModule {
  public static final String PREPROCESSOR_PARAM = "com.bbn.serif.preprocessorModules";
  public static final String POSTPROCESSOR_PARAM = "com.bbn.serif.postprocessorModules";

  private final Parameters params;

  public SerifEnvironmentM(final Parameters params) {
    this.params = checkNotNull(params);
  }

  @Override
  protected void configure() {
    install(new JacksonSerializationM());
    install(new LearningM(params));
    install(new SerifLanguage.Module(params));
    install(new SerifLocaleM(params));
    install(ModuleFromParameter.forParameter("com.bbn.serif.IOModule")
        .withDefault(SerifXMLIOFromParamsM.class).extractFrom(params));
    install(ModuleFromParameter.forParameter("com.bbn.serif.docTypeMapping.module")
        .withDefault(DefaultDocTypeMapperM.class).extractFrom(params));

    // this is a hack for getting far JARs working quickly for @msrivast
    // text-group/bue-common-open#57
    Multibinder.newSetBinder(binder(), JacksonSerializationM.jacksonModulesKey())
        .addBinding().toInstance(new BUECommonModule());

    // set up pre- and post-processors
    Multibinder.newSetBinder(binder(), preprocessorBindingKey());
    install(ModuleFromParameter.forMultiParameter(PREPROCESSOR_PARAM)
      .withNoOpDefault().extractFrom(params));

    Multibinder.newSetBinder(binder(), postprocessorBindingKey());
    install(ModuleFromParameter.forMultiParameter(POSTPROCESSOR_PARAM)
        .withNoOpDefault().extractFrom(params));
  }

  /**
   * Bind a {@link Multibinder} to this key to add preprocessors.
   */
  public static Key<ProcessingStep> preprocessorBindingKey() {
    return Key.get(ProcessingStep.class, DocTheoryPreprocessorP.class);
  }

  /**
   * Bind a {@link Multibinder} to this key to add postprocessors.
   */
  public static Key<ProcessingStep> postprocessorBindingKey() {
    return Key.get(ProcessingStep.class, DocTheoryPostprocessorP.class);
  }

  @Override
  public int hashCode() {
    return Objects.hash(params);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SerifEnvironmentM other = (SerifEnvironmentM) obj;
    return Objects.equals(this.params, other.params);
  }
}
