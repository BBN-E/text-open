package com.bbn.serif.relations;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.relations.constraints.ExactRelationMentionConstraint;
import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.RelationMention;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMention;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

@Beta
public final class RelationMentionFinderFromExactConstraints extends
    AbstractSentenceRelationMentionFinder {

  private static final Logger log =
      LoggerFactory.getLogger(RelationMentionFinderFromExactConstraints.class);
  private int successful = 0;
  private int failed = 0;

  @Inject
  private RelationMentionFinderFromExactConstraints(@TolerantP final boolean tolerant) {
    super(tolerant);
  }

  @Override
  protected RelationMentions relationMentionsForSentence(final DocTheory dt,
      final SentenceTheory sentenceTheory, final Set<RelationMentionConstraint> constraints) {
    final ImmutableList.Builder<RelationMention> ret = ImmutableList.builder();
    for (final RelationMentionConstraint constraint : constraints) {
      if (constraint instanceof ExactRelationMentionConstraint) {
        final ExactRelationMentionConstraint cons = (ExactRelationMentionConstraint) constraint;
        final Optional<Mention> left = extractMention(cons.left(), sentenceTheory);
        final Optional<Mention> right = extractMention(cons.right().orNull(), sentenceTheory);
        final ValueMention timeArg;
        if (cons.timeArg().isPresent()) {
          timeArg = extractTimeArg(cons.timeArg().get(), sentenceTheory);
        } else {
          timeArg = null;
        }
        if (right.isPresent() && left.isPresent()) {
          successful++;
          final RelationMention rm = new RelationMention.Builder()
              .type(cons.type())
              .rawType(cons.rawType())
              .leftMention(left.get())
              .rightMention(right.get())
              .timeArg(Optional.fromNullable(timeArg))
              .timeRole(cons.timeRole())
              .tense(cons.tense())
              .modality(cons.modality())
              .score(cons.score())
              .externalID(cons.externalID()).build();
          ret.add(rm);
        } else {
          failed++;
          final String missing;
          if (left.isPresent()) {
            missing = "right-" + cons.right();
          } else if (right.isPresent()) {
            missing = "left-" + cons.left();
          } else {
            missing = "both-" + cons.left() + ":" + cons.right();
          }
          if (tolerant) {
            log.warn("Unable to satisfy constraint {}, missing mention from relation {}", cons,
                missing);
          } else {
            throw new SerifException("Got an unfulfillable constraint, was missing " + missing
                + " mentions, are you running mention finding in a tolerant mode? Note that we discard RelationMentions where the right mention is a filler.");
          }
        }
      } else {
        throw new SerifException(
            "Got a constraint of type " + constraint.getClass() + " and cannot process it");
      }
    }
    return new RelationMentions.Builder().relationMentions(ret.build()).build();
  }

  private ValueMention extractTimeArg(final Symbol id, final SentenceTheory sentenceTheory) {
    checkNotNull(id);
    checkNotNull(sentenceTheory);
    for (final ValueMention vm : sentenceTheory.valueMentions()) {
      if (id.equalTo(vm.externalID().orNull())) {
        return vm;
      }
    }
    return null;
  }

  private Optional<Mention> extractMention(@Nullable final Symbol id,
      final SentenceTheory sentenceTheory) {
    if (id == null) {
      return Optional.absent();
    }
    checkNotNull(sentenceTheory);
    for (final Mention m : sentenceTheory.mentions()) {
      if (id.equalTo(m.externalID().orNull())) {
        return Optional.of(m);
      }
    }
    return Optional.absent();
  }

  @Override
  public void finish() {
    log.info("{} {} successful projections, {} failed projections", this.getClass(), successful, failed);
  }
}
