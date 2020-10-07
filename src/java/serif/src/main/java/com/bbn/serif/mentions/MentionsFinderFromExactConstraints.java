package com.bbn.serif.mentions;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.mentions.constraints.ExactMentionConstraint;
import com.bbn.serif.mentions.constraints.MentionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Mentions;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkState;

@Beta
public class MentionsFinderFromExactConstraints extends AbstractSentenceMentionFinder {

  private static final Logger log =
      LoggerFactory.getLogger(MentionsFinderFromExactConstraints.class);
  private int failed = 0;
  private int successful = 0;

  @Inject
  public MentionsFinderFromExactConstraints(@TolerantP final boolean tolerant) {
    super(tolerant);
  }

  @Override
  protected Mentions mentionsForSentence(final DocTheory docTheory, final SentenceTheory st,
      final Set<MentionConstraint> constraints) {
    final ImmutableList.Builder<Mention> mentions = ImmutableList.builder();

    final OffsetGroupRange range = st.sentence().locatedString().referenceBounds();
    for (final MentionConstraint constraint : constraints) {
      if (constraint instanceof ExactMentionConstraint) {
        final ExactMentionConstraint cons = (ExactMentionConstraint) constraint;
        // restricts the constraint to applying to this sentence.
        if (range.asCharOffsetRange().contains(cons.offsets().asCharOffsetRange())) {
          final Optional<SynNode> node = nodeForMention(st, cons.offsets().asCharOffsetRange());
          if (node.isPresent()) {
            if (node.get().mention().isPresent()) {
              failed++;
              if (tolerant) {
                log.warn("Warning, mention already exists for SynNode with constraint {}", cons);
                continue;
              } else {
                throw new SerifException("Mention already present for constraint " + cons);
              }
            }
            // strict checking that the spans line up.
            checkState(
                node.get().span().startToken().charOffsetRange().asRange().lowerEndpoint().asInt()
                    == cons.offsets().asCharOffsetRange().asRange().lowerEndpoint().asInt(),
                "Expected lower end point of constraint to match lower end point of synnode!");
            checkState(
                node.get().span().endToken().charOffsetRange().asRange().upperEndpoint().asInt()
                    == cons.offsets().asCharOffsetRange().asRange().upperEndpoint().asInt(),
                "Expected upper end point of SynNode to match upper endpoint of constraint!");
            final Mention m = node.get()
                .setMention(cons.mentionType(), cons.entityType(), cons.entitySubtype(), null, 1.0f,
                    1.0f, cons.externalID().orNull());
            mentions.add(m);
            successful++;
          } else if (tolerant) {
            failed++;
            log.warn("failed to enforce constraint {}, no matching SynNode found", cons);
          } else {
            throw new SerifException(
                "failed to use constraint {} " + cons + " unable to find a suitable SynNode");
          }
        } else {
          log.debug("ignoring constraint " + constraint + " as offsets do not match " + st);
        }
      } else {
        throw new SerifException(
            "Unknown constraint type: " + constraint.getClass() + " for constaint: " + constraint);
      }
    }
    log.debug("found {} mentions for {} from {} constraints", mentions.build().size(),
        docTheory.docid(),
        ImmutableList.copyOf(constraints).size());
    return new Mentions.Builder()
        .mentions(mentions.build())
        .parse(st.parse())
        .descScore(1.0f)
        .nameScore(1.0f).build();
  }

  // TODO move this somewhere shared
  public static Optional<SynNode> nodeForMention(final SentenceTheory st,
      final OffsetRange<CharOffset> offsets) {
    final List<SynNode> nodes = Lists.newArrayList();
    // if there is a mention in the sentence, it must have a non-empty parse
    //noinspection OptionalGetWithoutIsPresent
    nodes.add(st.parse().root().get());
    for (int i = 0; i < nodes.size(); i++) {
      final SynNode s = nodes.get(i);
      nodes.addAll(s.children());
      if (s.span().startToken().startCharOffset().asInt() == offsets.asRange().lowerEndpoint()
          .asInt() && s.span().endToken().endCharOffset().asInt() == offsets.asRange()
          .upperEndpoint().asInt()) {
        return Optional.of(s);
      }
    }
    return Optional.absent();
  }

  @Override
  public void finish() {
    log.info("{} {} successful projections, {} failed projections", this.getClass(), successful,
        failed);
  }
}
