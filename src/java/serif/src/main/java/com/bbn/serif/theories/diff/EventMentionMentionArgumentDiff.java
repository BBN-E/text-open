package com.bbn.serif.theories.diff;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;

import com.google.common.base.Optional;

import org.immutables.func.Functional;
import org.immutables.value.Value;

/**
 * Description of the difference between two {@link com.bbn.serif.theories.EventMention.MentionArgument}s.
 */
@Value.Immutable
@Functional
@TextGroupImmutable
public abstract class EventMentionMentionArgumentDiff implements EventMentionArgDiff<EventMention.MentionArgument> {
  @Override
  @Value.Parameter
  public abstract Optional<EventMention.MentionArgument> left();
  @Override
  @Value.Parameter
  public abstract Optional<EventMention.MentionArgument> right();

  @Override
  public abstract Optional<Difference<Symbol>> roleDiff();

  @Override
  public abstract Optional<Difference<Float>> scoreDiff();

  public abstract Optional<Difference<Mention>> mentionDiff();

  @Override
  public final void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    DiffUtils.writePropertyDiff("role", roleDiff(), sb, indentSpaces);
    DiffUtils.writePropertyDiff("mention", mentionDiff(), sb, indentSpaces);
    DiffUtils.writePropertyDiff("score", scoreDiff(), sb, indentSpaces);
  }

  public static class Builder extends ImmutableEventMentionMentionArgumentDiff.Builder {}
}
