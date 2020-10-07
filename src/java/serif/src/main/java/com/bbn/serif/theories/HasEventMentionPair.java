package com.bbn.serif.theories;

/**
 * Interface for anything which can provide a pair of event mentions (e.g. a candidate for creating a
 * {@link EventEventRelationMention}.  The implementing class is responsible for deciding on some consistent
 * way of assigning the two event mentions to the "first" and "second" slot. There is no guarantee the
 * {@link EventMention}s come from the same sentence but they should be from the same {@link DocTheory}.
 *
 * Each {@link EventMention} should be from its corresponding {@link SentenceTheory} and both
 * {@link SentenceTheory}s should be from {@link #docTheory()}.
 */
public interface HasEventMentionPair extends HasDocTheory, HasSpanningPair {

  @Override
  EventMention firstSpanning();

  /**
   * The {@link SentenceTheory} containing {@link #firstSpanning()}
   */
  @Override
  SentenceTheory firstSpanningSentence();

  @Override
  EventMention secondSpanning();

  /**
   * The {@link SentenceTheory} containing {@link #secondSpanning()}
   */
  @Override
  SentenceTheory secondSpanningSentence();
}
