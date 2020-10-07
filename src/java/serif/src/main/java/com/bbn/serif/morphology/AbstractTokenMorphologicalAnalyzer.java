package com.bbn.serif.morphology;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.MorphToken;
import com.bbn.serif.theories.MorphTokenSequence;
import com.bbn.serif.theories.MorphologyAlgorithmDescription;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;

/**
 * Convenience class for implemented morphological analyzers which analyze each token independently
 * of all others.
 */
public abstract class AbstractTokenMorphologicalAnalyzer
    extends AbstractSentenceMorphologicalAnalyzer {

  /**
   * Returns a copy of the input {@link DocTheory} augmented with morphological analyses of its
   * tokens. There is no guarantee that all (or any) tokens will receive an analysis.
   */
  public final SentenceTheory analyze(DocTheory dt, SentenceTheory st) {
    final MorphologyAlgorithmDescription algorithm = algorithmDescription();

    final SentenceTheory.Builder ret = st.modifiedCopyBuilder();
    final MorphTokenSequence.Builder morphTokenSequenceB =
        new MorphTokenSequence.Builder()
            .sourceAlgorithm(algorithm)
            .tokenSequence(st.tokenSequence());

    for (final Token token : st.tokenSequence()) {
      morphTokenSequenceB.addMorphTokens(analyzeToken(token));
    }

    return ret.addMorphTokenSequences(morphTokenSequenceB.build()).build();
  }

  /**
   * The {@link MorphologyAlgorithmDescription} to be attached to added {@link
   * MorphTokenSequence}s.  For efficiency, this should typically return a precomputed constant.
   *
   * Note the implementing sub-class is responsible for attaching the source algorithm to objects it
   * creates (e.g. {@link com.bbn.serif.theories.MorphTokenAnalysis}.
   */
  protected abstract MorphologyAlgorithmDescription algorithmDescription();

  /**
   * Returns the morphological analysis for a single token.
   *
   * Note the implementing sub-class is responsible for attaching the source algorithm to objects it
   * creates (e.g. {@link com.bbn.serif.theories.MorphTokenAnalysis}.
   */
  protected abstract MorphToken analyzeToken(Token token);
}
