package com.bbn.serif.names;

import com.bbn.serif.names.constraints.NameConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.collect.Iterables;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AbstractConstraintlessSentenceNameFinder extends AbstractSentenceNameFinder {

  protected AbstractConstraintlessSentenceNameFinder() {
    // tolerance parameter doesn't matter because we don't have constraints anyway
    super(false);
  }

  @Override
  public SentenceTheory addNames(DocTheory docTheory, SentenceTheory sentenceTheory,
      Set<NameConstraint> constraints) {
    checkArgument(Iterables.isEmpty(constraints), "This namefinder does not support constraints");
    return addNames(docTheory, sentenceTheory);
  }
}
