package com.bbn.serif.relations;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.io.SerifXMLException;
import com.bbn.serif.relations.constraints.ExactRelationConstraint;
import com.bbn.serif.relations.constraints.RelationConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Relation;
import com.bbn.serif.theories.RelationMention;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.Relations;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

public final class RelationFinderFromExactConstraints implements RelationFinder {

  private static final Logger log =
      LoggerFactory.getLogger(RelationFinderFromExactConstraints.class);

  private final boolean tolerant;
  private int failed = 0;
  private int successful = 0;

  @Inject
  private RelationFinderFromExactConstraints(@TolerantP final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public DocTheory addRelations(final DocTheory docTheory,
      final Set<RelationConstraint> constraints) {
    final DocTheory.Builder ret = docTheory.modifiedCopyBuilder();
    final ImmutableList.Builder<Relation> relations = ImmutableList.builder();
    for (final RelationConstraint constraint : constraints) {
      if (constraint instanceof ExactRelationConstraint) {
        final ExactRelationConstraint cons = (ExactRelationConstraint) constraint;
        if (cons.right() != null) {
          final Entity left = extractEntity(cons.left(), docTheory);
          final Entity right = extractEntity(cons.right(), docTheory);
          final RelationMentions rms = extractMentions(cons.relationMentionIDs(), docTheory);
          final Symbol type = cons.type();
          if (left != null && right != null && rms.size() > 0) {
            successful++;
            relations.add(Relation.create(left, right, rms, type, cons.tense(), cons.modality(),
                cons.confidence(), cons.externalID()));
          } else {
            failed++;
            final String missing;
            if (right != null) {
              missing = "left-" + cons.left().asString();
            } else if (left != null) {
              missing = "right-" + cons.right().asString();
            } else if (rms.size() == 0) {
              missing = "relation_mentions";
            } else {
              missing = "both" + cons.left() + "-" + cons.right();
            }

            if (tolerant) {
              log.warn("dropping relation {}, could not find {}", cons.externalID(),
                  missing);
            } else {
              throw new SerifException(
                  "dropping relation for " + cons.externalID() + " could not find " + missing);
            }
          }
        }
      }
    }
    final DocTheory out = ret.relations(Relations.create(relations.build())).build();
    for (final RelationConstraint constraint : constraints) {
      if (!constraint.satisfiedBy(out)) {
        if (tolerant) {
          log.debug("Could not satisfy RelationConstraint " + constraint);
        } else {
          throw new SerifXMLException("Could not satisfy RelationConstaint " + constraint);
        }
      }
    }
    log.info("For doc {}, found {} relations from {} constraints", out.docid(),
        out.relations().asList().size(),
        ImmutableSet.copyOf(constraints).size());
    return out;
  }

  private RelationMentions extractMentions(final ImmutableSet<Symbol> rmIDs,
      final DocTheory docTheory) {
    final ImmutableList.Builder<RelationMention> ret = ImmutableList.builder();
    for (final SentenceTheory st : docTheory.nonEmptySentenceTheories()) {
      for (final RelationMention rm : st.relationMentions()) {
        for (final Symbol rmID : rmIDs) {
          if (rm.externalID().isPresent()) {
            if (rm.externalID().get().equals(rmID)) {
              ret.add(rm);
            }
          } else {
            throw new SerifException(
                "Excepted external IDs on relationmentions but not found on " + rm);
          }
        }
      }
    }
    return new RelationMentions.Builder().relationMentions(ret.build()).build();
  }

  private Entity extractEntity(final Symbol id, final DocTheory docTheory) {
    for (final Entity e : docTheory.entities()) {
      if (e.externalID().isPresent()) {
        if (e.externalID().get().equalTo(id)) {
          return e;
        }
      } else {
        throw new SerifException("Expected external ids to be present but none found for " + e);
      }
    }
    return null;
  }


  @Override
  public void finish() throws IOException {
    log.info("{} {} successful projections, {} failed projections", this.getClass(), successful,
        failed);
  }
}
