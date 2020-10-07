package com.bbn.serif.values;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.ValueMentions;
import com.bbn.serif.types.ValueType;
import com.bbn.serif.values.constraints.ExactValueMentionConstraint;
import com.bbn.serif.values.constraints.Timex2Constraint;
import com.bbn.serif.values.constraints.ValueMentionConstraint;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import javax.inject.Inject;

@Beta
public class ValueMentionFinderFromExactConstraints extends AbstractSentenceValuementionFinder {

  private static final Logger log =
      LoggerFactory.getLogger(ValueMentionFinderFromExactConstraints.class);

  // TODO is this the right type?
  private static ValueType timexType = ValueType.parseDottedPair(Symbol.from("TIMEX2").asString());

  private int successful = 0;
  private int failed = 0;

  @Inject
  public ValueMentionFinderFromExactConstraints(@TolerantP final boolean tolerant) {
    super(tolerant);
  }

  @Override
  protected ValueMentions valueMentionsForSentence(final SentenceTheory sentenceTheory,
      final Set<ValueMentionConstraint> constraints) {
    final OffsetRange<CharOffset>
        offsets = sentenceTheory.sentence().locatedString().referenceBounds().asCharOffsetRange();
    final ImmutableList.Builder<ValueMention> mentions = ImmutableList.builder();
    for (final ValueMentionConstraint constraint : constraints) {
      if (constraint instanceof Timex2Constraint) {
        final Timex2Constraint cons = (Timex2Constraint) constraint;
        if (offsets.contains(cons.offsets())) {
          final ValueMention mention = ValueMention.builder(timexType,
              sentenceTheory.tokenSequence().spanFromCharacterOffsets(cons.offsets()).get())
              .setExternalID(cons.externalID().orNull()).build();

          // TODO is this the right thing?
          mention.setDocValue(cons.timexVal(), cons.anchorVal().orNull(),
              cons.anchorDirection().orNull(), cons.timexSet().orNull(), cons.timexMode().orNull(),
              cons.timexNonSpecific().orNull());
          mentions.add(mention);
        } else {
          log.debug(
              "Timex2Constraint " + cons + " falls outside of SentenceTheory " + sentenceTheory);
        }
      } else if (constraint instanceof ExactValueMentionConstraint) {
        final ExactValueMentionConstraint cons = (ExactValueMentionConstraint) constraint;
        if (offsets.contains(cons.offsets())) {
          successful++;
          final ValueType valueType = ValueType.parseDottedPair(cons.type().asString());
          final Optional<TokenSequence.Span> span =
              sentenceTheory.tokenSequence().spanFromCharacterOffsets(cons.offsets());
          if (span.isPresent()) {
            final ValueMention mention = ValueMention.builder(valueType,
                span.get())
                .setExternalID(cons.externalID().orNull()).build();
            mentions.add(mention);
          } else {
            failed++;
            if (tolerant) {
              log.warn("failed to find a span for value mention constraint {} sentenceTheory {}",
                  cons, sentenceTheory);
            } else {
              throw new SerifException("failed to find a span for value mention constraint " + cons
                  + " for sentence theory " + sentenceTheory);
            }
          }
        } else {
          log.debug("ExactValueMentionConstraint " + cons + " falls outside of SentenceTheory "
              + sentenceTheory);
        }
      } else {
        throw new RuntimeException("Got a constraint of type " + constraint.getClass()
            + " but expected only Timex2Constraints or ExactValueMentionConstraints");
      }
    }
    return ValueMentions.create(mentions.build());
  }

  @Override
  public void finish() {
    log.info("{} {} successful projections, {} failed", this.getClass(), successful, failed);
  }
}
