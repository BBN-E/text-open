package com.bbn.serif.theories;

/**
 * Interface for anything which can provide a pair of Spannings (e.g. a candidate for creating a
 * {@link RelationMention}.
 */
public interface HasSpanningPair extends HasDocTheory {
  Spanning firstSpanning();

  SentenceTheory firstSpanningSentence();

  Spanning secondSpanning();

  SentenceTheory secondSpanningSentence();
}
