package com.bbn.serif.morphology;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.ModuleFromParameter;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

/**
 * Object which can augment a document with morphological analyses of its tokens.
 *
 * Note that an implementation is not guaranteed to analyze all or even any tokens.
 *
 * A morphological analyzer may be injected by specifying the
 * {@code com.bbn.serif.morphologicalAnalyzer} parameter. See {@link FromParamsModule}.
 */
public interface MorphologicalAnalyzer {

  DocTheory analyze(final DocTheory docTheory);

  SentenceTheory analyze(final DocTheory docTheory, final SentenceTheory sentenceTheory);

  class FromParamsModule extends AbstractParameterizedModule {

    protected FromParamsModule(final Parameters parameters) {
      super(parameters);
    }

    @Override
    public void configure() {
      install(ModuleFromParameter.forParameter("com.bbn.serif.morphology.morphologicalAnalyzer")
          .extractFrom(params()));
    }
  }
}
