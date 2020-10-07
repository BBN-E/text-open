package com.bbn.serif.names;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.names.constraints.NameConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

@Beta
public abstract class AbstractSentenceNameFinder implements NameFinder {

  private final Logger log;

  protected final boolean tolerant;

  protected AbstractSentenceNameFinder(@TolerantP final boolean tolerant) {
    this.tolerant = tolerant;
    this.log = LoggerFactory.getLogger(this.getClass());
  }

  @Override
  public DocTheory addNames(final DocTheory docTheory) {
    return addNames(docTheory, ImmutableSet.<NameConstraint>of());
  }

  @Override
  public DocTheory addNames(final DocTheory docTheory,
      final Set<NameConstraint> constraints) {
    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();

    final ImmutableSet.Builder<Name> originalNamesB = ImmutableSet.builder();
    final ImmutableSet.Builder<Name> newNamesB = ImmutableSet.builder();

    for (int i = 0; i < docTheory.numSentences(); ++i) {
      final SentenceTheory st = docTheory.sentenceTheory(i);
      if (!st.isEmpty()) {
        originalNamesB.addAll(st.names());
        final SentenceTheory stWithNames = addNames(docTheory, st, constraints);
        newNamesB.addAll(stWithNames.names());
        ret.replacePrimarySentenceTheory(st, stWithNames);
      }
    }

    final DocTheory out = ret.build();
    for (final NameConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.warn("Constraint " + constraint + " violated by " + out);
        } else {
          throw new SerifException("Constraint " + constraint + " violated by " + out);
        }
      }
    }

    final ImmutableSet<Name> oldNames = originalNamesB.build();
    final ImmutableSet<Name> newNames = newNamesB.build();

    final Set<Name> addedNames = Sets.difference(newNames, oldNames);
    final Set<Name> deleteNames = Sets.difference(oldNames, newNames);

    if (!addedNames.isEmpty()) {
      log.debug("Added {} names: {}", addedNames.size(), addedNames);
    }
    if (!deleteNames.isEmpty()) {
      log.debug("Delete {} names: {}", deleteNames.size(), deleteNames);
    }

    return out;
  }

  @Override
  public abstract SentenceTheory addNames(DocTheory docTheory, SentenceTheory sentenceTheory,
      Set<NameConstraint> constraints);

  @Override
  public void finish() throws IOException {
    // do nothing
  }
}

