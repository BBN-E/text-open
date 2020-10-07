package com.bbn.serif.entities;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.entities.constraints.EntityConstraint;
import com.bbn.serif.entities.constraints.ExternalIDEntityConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entities;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.MentionConfidence;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

@Beta
public final class EntityFinderFromExternalIDs implements EntityFinder {

  private static final Logger log = LoggerFactory.getLogger(EntityFinderFromExternalIDs.class);

  private final boolean tolerant;
  private int successful = 0;
  private int failed = 0;

  @Inject
  private EntityFinderFromExternalIDs(@TolerantP final boolean tolerant) {
    this.tolerant = tolerant;
  }

  public static EntityFinderFromExternalIDs create(final boolean tolerant) {
    return new EntityFinderFromExternalIDs(tolerant);
  }

  @Override
  public DocTheory addEntities(final DocTheory input,
      final Set<EntityConstraint> constraints) {
    final DocTheory.Builder ret = input.modifiedCopyBuilder();

    final ImmutableList.Builder<Entity> entities = ImmutableList.builder();
    for (final EntityConstraint entityConstraint : constraints) {
      if (entityConstraint instanceof ExternalIDEntityConstraint) {
        final ExternalIDEntityConstraint cons = (ExternalIDEntityConstraint) entityConstraint;
        final ImmutableSet<Mention> mentions = extractMentions(input, cons.mentionIDs());
        final ImmutableMap<Mention, MentionConfidence> confidences =
            extractMentionConfidences(mentions);
        if (mentions.size() > 0) {
          entities.add(Entity
              .create(mentions, cons.entityType(), cons.entitySubtype(), Entity.UNASSIGNED_GUID,
                  cons.generic(), confidences, cons.externalID()));
          successful++;
        } else {
          failed++;
          if (tolerant) {
            log.warn("failed to create entity from constraint {}, no mentions found",
                entityConstraint);
          } else {
            throw new SerifException("failed to create entity from contraint " + entityConstraint
                + " no mentions found, perhaps they were pruned?");
          }
        }
      } else {
        throw new SerifException(
            "Can only handle ExactEntityConstraints but got " + entityConstraint + " of type "
                + entityConstraint.getClass());
      }
    }
    ret.entities(Entities.create(entities.build(), "1.0"));

    final DocTheory out = ret.build();
    for (final EntityConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.warn("Constraint " + constraint + " violated by " + out);
        } else {
          throw new SerifException("Constraint " + constraint + " violated by " + out);
        }
      }
    }
    log.debug("added {} entities to {} with {} constraints", out.entities().size(), out.docid(),
        ImmutableList.copyOf(constraints).size());
    return out;
  }

  private ImmutableMap<Mention, MentionConfidence> extractMentionConfidences(
      final ImmutableSet<Mention> mentions) {
    final ImmutableMap.Builder<Mention, MentionConfidence> ret = ImmutableMap.builder();
    for (final Mention m : mentions) {
      ret.put(m, MentionConfidence.DEFAULT);
    }
    return ret.build();
  }

  private ImmutableSet<Mention> extractMentions(final DocTheory input,
      final ImmutableSet<Symbol> ids) {
    final ImmutableSet.Builder<Mention> mentionBuilder = ImmutableSet.builder();
    for (final SentenceTheory st : input.nonEmptySentenceTheories()) {
      for (final Mention m : st.mentions()) {
        if (m.externalID().isPresent()) {
          if (ids.contains(m.externalID().get())) {
            mentionBuilder.add(m);
          }
        } else {
          throw new SerifException("Mention IDs must be set, wasn't for " + m + " in " + st);
        }
      }
    }
    return mentionBuilder.build();
  }

  @Override
  public void finish() throws IOException {
    log.info("{}: {} successful projections, {} failed", this.getClass(), successful, failed);
  }
}
