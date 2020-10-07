package com.bbn.serif.morphology;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

/**
 * A {@link MorphologicalAnalyzer} which works over each sentence independently.
 */
public abstract class AbstractSentenceMorphologicalAnalyzer implements MorphologicalAnalyzer {

  @Override
  public DocTheory analyze(final DocTheory docTheory) {
    return docTheory.copyWithTransformedPrimarySentenceTheories(
        new DocTheory.SentenceTheoryTransformer() {
          @Override
          public SentenceTheory transform(final DocTheory dt, final SentenceTheory st) {
            return analyze(dt, st);
          }
        }
    );
  }
}
