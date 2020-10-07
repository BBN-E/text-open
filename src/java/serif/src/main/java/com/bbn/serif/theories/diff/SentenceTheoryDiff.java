package com.bbn.serif.theories.diff;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.List;

/**
 * Represents the differences between two sentence theories. Currently only covers the event
 * mentions.
 */
@Beta
@Value.Immutable
@Functional
@TextGroupImmutable
public abstract class SentenceTheoryDiff implements Difference<SentenceTheory> {
  @Override
  @Value.Parameter
  public abstract Optional<SentenceTheory> left();
  @Override
  @Value.Parameter
  public abstract Optional<SentenceTheory> right();
  public abstract Optional<CollectionDifference<List<Name>, Name, NameDiff>> nameDiffs();
  public abstract Optional<CollectionDifference<List<EventMention>, EventMention, EventMentionDiff>> eventMentionDiffs();

  @Override
  public final void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    if (nameDiffs().isPresent() || eventMentionDiffs().isPresent()) {
      final UnicodeFriendlyString sentenceText;
      sentenceText = left().get().span().rawOriginalText();
      sb.append(Strings.repeat(" ", indentSpaces)).append(sentenceText).append("\n");
    }
    if (nameDiffs().isPresent()) {
      sb.append(Strings.repeat(" ", indentSpaces+2)).append("names:");
      nameDiffs().get().writeTextReport(sb, indentSpaces+4);
    }
    if (eventMentionDiffs().isPresent()) {
      sb.append(Strings.repeat(" ", indentSpaces+2)).append("eventMentions:");
      eventMentionDiffs().get().writeTextReport(sb, indentSpaces+4);
    }
  }

  public static class Builder extends ImmutableSentenceTheoryDiff.Builder {}
}
