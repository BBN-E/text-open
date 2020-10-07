package com.bbn.serif.events;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.events.constraints.EventMentionConstraint;
import com.bbn.serif.events.constraints.ExactEventMentionConstraint;
import com.bbn.serif.mentions.MentionFinder;
import com.bbn.serif.mentions.MentionsFinderFromExactConstraints;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.EventMentions;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.ValueMention;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

@Beta
public final class EventMentionFinderFromExternalIDs extends AbstractSentenceEventMentionFinder {

  private static final Logger log = LoggerFactory.getLogger(EventMentionFinderFromExternalIDs.class);

  private int failed = 0;
  private int successful = 0;

  @Inject
  EventMentionFinderFromExternalIDs(@MentionFinder.TolerantP final boolean tolerant) {
    super(tolerant);
  }

  public static EventMentionFinderFromExternalIDs create(final boolean tolerant) {
    return new EventMentionFinderFromExternalIDs(tolerant);
  }

  @Override
  protected EventMentions eventMentionsFromConstraints(final SentenceTheory sentenceTheory,
      final Set<EventMentionConstraint> constraints) {
    final ImmutableList.Builder<EventMention> mentions = ImmutableList.builder();
    for (final EventMentionConstraint constraint : constraints) {
      if (constraint instanceof ExactEventMentionConstraint) {
        final ExactEventMentionConstraint cons = (ExactEventMentionConstraint) constraint;
        final EventMention.Builder em = EventMention.builder(cons.eventType());
        em.setExternalID(cons.externalID().orNull());

        final Optional<SynNode> anchorNode = MentionsFinderFromExactConstraints.nodeForMention(sentenceTheory, cons.triggerOffsets());
        if(anchorNode.isPresent()) {
          successful++;
          em.setAnchorNode(anchorNode.get());
        } else {
          failed++;
          if(tolerant) {
            log.warn("failed to map constraint {}, unable to locate anchor node!", cons);
            continue;
          } else {
            throw new SerifException("Unable to use constraint " + cons + "; SynNode not found");
          }
        }

        em.setAnchorProp(cons.anchorProp());
        em.setArguments(extractArguments(sentenceTheory, cons));
        em.setGenericity(cons.genericity(), cons.genericityScore());
        em.setModality(cons.modality(), cons.modalityScore());
        em.setPolarity(cons.polarity());
        em.setTense(cons.tense());
        mentions.add(em.build());
      }
    }
    return new EventMentions.Builder().eventMentions(mentions.build()).build();
  }

  private List<EventMention.Argument> extractArguments(final SentenceTheory sentenceTheory,
      final ExactEventMentionConstraint cons) {
    final ImmutableList.Builder<EventMention.Argument> ret = ImmutableList.builder();
    final ImmutableSet<Symbol> mentionIDs = cons.mentionIDs();
    for (final Mention m : sentenceTheory.mentions()) {
      if (m.externalID().isPresent()) {
        if (mentionIDs.contains(m.externalID().get())) {
          ret.add(EventMention.MentionArgument
              .from(cons.idToRole().get(m.externalID().get()), m, 1.0f));
        }
      } else {
        throw new SerifException("Expected external IDs defined but weren't for " + m);
      }
    }
    final ImmutableSet<Symbol> valueMentionIds = cons.valueMentionIDs();
    for (final ValueMention vm : sentenceTheory.valueMentions()) {
      if (vm.externalID().isPresent()) {
        if (valueMentionIds.contains(vm.externalID().get())) {
          ret.add(EventMention.ValueMentionArgument
              .from(cons.idToRole().get(vm.externalID().get()), vm, 1.0f));
        }
      } else {
        throw new SerifException("Expected external IDs defined but weren't for " + vm);
      }
    }
    checkArgument(cons.spanArguments().size() == 0, "implement span argument handling!");
    return ret.build();
  }

  @Override
  public void finish() throws IOException {
    log.info("{} {} successful projections, {} failed", this.getClass(), successful, failed);
  }
}
