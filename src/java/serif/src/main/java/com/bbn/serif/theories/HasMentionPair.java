package com.bbn.serif.theories;

/**
 * Interface for anything which can provide a pair of mentions (e.g. a candidate for creating a
 * {@link RelationMention}.  The implementing class is responsible for deciding on some consistent
 * way of assigning the two mentions to the "first" and "second" slot. There is no guarantee the
 * {@link Mention}s come from the same sentence but they should be from the same {@link DocTheory}.
 *
 * Each {@link Mention} should be from its corresponding {@link SentenceTheory} and both
 * {@link SentenceTheory}s should be from {@link #docTheory()}.
 */
public interface HasMentionPair extends HasDocTheory, HasSpanningPair {

  @Override
  Mention firstSpanning();

  /**
   * The {@link SentenceTheory} containing {@link #firstSpanning()}
   */
  @Override
  SentenceTheory firstSpanningSentence();

  @Override
  Mention secondSpanning();

  /**
   * The {@link SentenceTheory} containing {@link #secondSpanning()}
   */
  @Override
  SentenceTheory secondSpanningSentence();
}
