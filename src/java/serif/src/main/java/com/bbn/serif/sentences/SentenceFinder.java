package com.bbn.serif.sentences;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.sentences.constraints.SentenceSegmentationConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.util.Set;

/**
 * Segments a document into sentences.  Input documents should not already have sentences or
 * an {@link IllegalArgumentException} should be thrown.
 *
 * The implementation is responsible for verifying that the constraints are obeyed and crashing or
 * issuing a warning as appropriate.
 */
@Beta
public interface SentenceFinder extends Finishable {

  DocTheory segmentSentences(DocTheory docTheory);

  DocTheory segmentSentences(DocTheory docTheory, Set<SentenceSegmentationConstraint> constraints);
}


