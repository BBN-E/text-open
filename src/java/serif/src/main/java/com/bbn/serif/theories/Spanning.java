package com.bbn.serif.theories;

/**
 * Any object which has a defined token span within a sentence.
 *
 * @author rgabbard
 */
public interface Spanning extends TokenSpanning {
  TokenSequence.Span span();
}
