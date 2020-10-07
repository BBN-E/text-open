package com.bbn.serif.names;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.names.constraints.ExactNameConstraint;
import com.bbn.serif.names.constraints.NameConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.Names;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.TokenSequence;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Adds only those names specified by {@link com.bbn.serif.names.constraints.ExactNameConstraint}s.
 */
@Beta
public final class NameFinderFromExactConstraints extends AbstractSentenceNameFinder {

  private static final Logger log = LoggerFactory.getLogger(NameFinderFromExactConstraints.class);

  private int failures = 0;
  private int successful = 0;

  @Inject
  NameFinderFromExactConstraints(final @TolerantP boolean tolerant) {
    super(tolerant);
  }

  public static NameFinderFromExactConstraints createToleratingErrors() {
    return new NameFinderFromExactConstraints(true);
  }

  @Override
  public SentenceTheory addNames(final DocTheory docTheory, final SentenceTheory sentenceTheory) {
    // if we have no constraints, we have nothing to do
    return sentenceTheory;
  }

  @Override
  public SentenceTheory addNames(final DocTheory docTheory, final SentenceTheory sentenceTheory,
      final Set<NameConstraint> constraints) {
    final List<Name> names = Lists.newArrayList();

    // TODO handle restricting these constraints to a particular SentenceTheory
    for (final NameConstraint constraint : constraints) {
      addNameForConstraint(sentenceTheory, constraint, names);
    }

    return sentenceTheory.modifiedCopyBuilder().withNameTheory(
        Names.createFrom(names, sentenceTheory.tokenSequence(), 1.0)).build();
  }

  private void addNameForConstraint(final SentenceTheory sentenceTheory,
      final NameConstraint constraint, final List<Name> names) {
    if (constraint instanceof ExactNameConstraint) {
      final ExactNameConstraint enc = ((ExactNameConstraint) constraint);
      final Optional<TokenSequence.Span> spanOpt = sentenceTheory.tokenSequence()
          .spanFromCharacterOffsets(enc.offsets().asCharOffsetRange());
      if (spanOpt.isPresent()) {
        names.add(Name.of(enc.entityType(), spanOpt.get())
            .withExternalID(enc.externalID()));
        successful++;
      } else {
        if (tolerant) {
          log.info("Failed to project exact name constraint {}", enc);
          ++failures;
        } else {
          throw new SerifException("Failed to project exact name constraint " + enc);
        }
      }
    } else {
      throw new SerifException(
          "Cannot handle anything except ExactNameConstraints but got " + constraint + " of type"
              + constraint.getClass());
    }
  }

  @Override
  public void finish() {
    log.info("{} {} successful projections, {} failed projections", this.getClass(), successful, failures);
  }
}
