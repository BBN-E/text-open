package com.bbn.serif.theories.diff;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.types.GainLoss;
import com.bbn.serif.types.Genericity;
import com.bbn.serif.types.Indicator;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Polarity;
import com.bbn.serif.types.Tense;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.List;

/**
 * The difference between two event mentions.
 */
@Value.Immutable
@Functional
@TextGroupImmutable
abstract class EventMentionDiff implements Difference<EventMention>, WithEventMentionDiff {
  @Value.Parameter
  @Override
  public abstract Optional<EventMention> left();
  @Value.Parameter
  @Override
  public abstract Optional<EventMention> right();


  public abstract Optional<Difference<Symbol>> eventTypeDiff();

  public abstract Optional<CollectionDifference<List<EventMention.Argument>, EventMention.Argument, Difference<EventMention.Argument>>> argumentDiffs();
  public abstract Optional<Difference<Symbol>> patternIDDiff();
  public abstract Optional<Difference<Modality>> modalityDiff();
  public abstract Optional<Difference<Double>> modalityScoreDiff();
  public abstract Optional<Difference<Polarity>> polarityDiff();
  public abstract Optional<Difference<Genericity>> genericityDiff();
  public abstract Optional<Difference<Double>> genericityScoreDiff();
  public abstract Optional<Difference<Tense>> tenseDiff();
  public abstract Optional<Difference<Proposition>> anchorPropDiff();
  public abstract Optional<Difference<SynNode>> anchorNodeDiff();
  public abstract Optional<Difference<Indicator>> indicatorDiff();
  public abstract Optional<Difference<GainLoss>> gainLossDiff();
  public abstract Optional<Difference<Double>> scoreDiff();

  @Override
  public final void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    sb.append(Strings.repeat(" ", indentSpaces)).append("EventMention:\n");
    if (!eventTypeDiff().isPresent()) {
      sb.append(Strings.repeat(" ", indentSpaces + 2)).append("eventType: ")
          .append(left().get().type()).append("\n");
    } else {
      DiffUtils.writePropertyDiff("eventType", eventTypeDiff(), sb, indentSpaces + 2);
    }
    if (!anchorNodeDiff().isPresent()) {
      sb.append(Strings.repeat(" ", indentSpaces + 2)).append("anchorNode: ")
        .append(left().get().anchorNode()).append("\n");
    } else {
      DiffUtils.writePropertyDiff("anchorNode", anchorNodeDiff(), sb, indentSpaces+2);
    }
    DiffUtils.writePropertyDiff("modality", modalityDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("modality score", modalityDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("polarity", polarityDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("genericity", genericityDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("genericity score", genericityScoreDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("tense", tenseDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("indicator", indicatorDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("gainLoss", gainLossDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("score", scoreDiff(), sb, indentSpaces+2);
    DiffUtils.writePropertyDiff("arguments", argumentDiffs(), sb, indentSpaces+2);
  }

  public static class Builder extends ImmutableEventMentionDiff.Builder {}
}

