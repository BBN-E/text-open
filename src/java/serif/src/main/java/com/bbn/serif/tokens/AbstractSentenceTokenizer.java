package com.bbn.serif.tokens;

import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheoryBeam;
import com.bbn.serif.tokens.constraints.TokenizationConstraint;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Promotes a tokenizer which can work over just sentences to one which can work over
 * {@link DocTheory}s.
 */
abstract class AbstractSentenceTokenizer implements TokenFinder {

  @Override
  public final DocTheory tokenize(DocTheory docTheory) {
    return tokenize(docTheory, ImmutableSet.<TokenizationConstraint>of());
  }

  @Override
  public final DocTheory tokenize(DocTheory docTheory,
      final Set<TokenizationConstraint> constraints) {
    if (!docTheory.sentenceTheories().isEmpty()) {
      throw new IllegalArgumentException("Input document to tokenizer is already tokenized");
    }

    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();

    // we know by the check above that all of these beams lack sentence theories
    final List<SentenceTheoryBeam> newSentenceBeams =
        new ArrayList<>(docTheory.sentenceTheoryBeams());

    for (int i = 0; i < newSentenceBeams.size(); ++i) {
      final SentenceTheoryBeam originalBeam = newSentenceBeams.get(i);
      newSentenceBeams.set(i, originalBeam.copyWithPrimaryTheoryReplaced(
          tokenize(docTheory.document(), originalBeam.sentence(), constraints)));
    }
    ret.sentenceTheoryBeams(newSentenceBeams);

    return ret.build();
  }
}
