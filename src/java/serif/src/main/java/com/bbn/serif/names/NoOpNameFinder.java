package com.bbn.serif.names;

import com.bbn.serif.names.constraints.NameConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.inject.AbstractModule;

import java.io.IOException;
import java.util.Set;

/**
 * A name finder which never finds any names. This is what we return if we get no training data.
 */
public final class NoOpNameFinder implements NameFinder {

  private NoOpNameFinder() {
  }

  public static NoOpNameFinder create() {
    return new NoOpNameFinder();
  }

  @Override
  public DocTheory addNames(final DocTheory input) {
    return input;
  }

  @Override
  public DocTheory addNames(final DocTheory input, final Set<NameConstraint> constraints) {
    return input;
  }

  @Override
  public SentenceTheory addNames(final DocTheory docTheory, final SentenceTheory sentenceTheory) {
    return sentenceTheory;
  }

  @Override
  public SentenceTheory addNames(final DocTheory docTheory, final SentenceTheory sentenceTheory,
      final Set<NameConstraint> constraints) {
    return sentenceTheory;
  }

  @Override
  public void finish() throws IOException {

  }

}
