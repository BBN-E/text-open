package com.bbn.serif.sentences.constraints;


import com.bbn.bue.common.strings.LocatedString;
import com.bbn.serif.theories.Sentence;

import com.google.common.annotations.Beta;

@Beta
public interface SentenceSegmentationConstraint {

  /**
   * @param text - the source original text, to allow for constraints relative to the source text.
   * @param sentence - the output sentence, to allow for local constraints
   * @return
   */
  boolean satisfiedBy(final LocatedString text, final Sentence sentence);
}
