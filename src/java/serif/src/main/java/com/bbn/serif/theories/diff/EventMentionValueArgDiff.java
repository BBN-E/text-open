package com.bbn.serif.theories.diff;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.ValueMention;

import com.google.common.base.Optional;

import org.immutables.func.Functional;
import org.immutables.value.Value;

/**
 * Represents the differences between two value mention event mention arguments. Not currently used.
 */
@Value.Immutable
@Functional
@TextGroupImmutable
public abstract class EventMentionValueArgDiff
    implements EventMentionArgDiff<EventMention.ValueMentionArgument> {
  @Override
  @Value.Parameter
  public abstract Optional<EventMention.ValueMentionArgument> left();
  @Override
  @Value.Parameter
  public abstract Optional<EventMention.ValueMentionArgument> right();
  @Override
  public abstract Optional<Difference<Symbol>> roleDiff();
  @Override
  public abstract Optional<Difference<Float>> scoreDiff();
  public abstract Optional<Difference<ValueMention>> valueMentionDiff();

  @Override
  public void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    DiffUtils.writePropertyDiff("role", roleDiff(), sb, indentSpaces);
    DiffUtils.writePropertyDiff("valueMention", valueMentionDiff(), sb, indentSpaces);
    DiffUtils.writePropertyDiff("score", scoreDiff(), sb, indentSpaces);
  }

  public static class Builder extends ImmutableEventMentionValueArgDiff.Builder {}
}
