package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Tense;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import org.immutables.value.Value;

import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;

@TextGroupImmutable
@Value.Immutable
@JsonSerialize
@JsonDeserialize(as = ImmutableRelationMention.class)
public abstract class RelationMention implements Spanning, HasExternalID, WithRelationMention {

  public static final Symbol ARG1 = Symbol.from("ARG1");
  public static final Symbol ARG2 = Symbol.from("ARG2");

  public abstract Symbol type();

  public abstract Optional<Symbol> rawType();

  public abstract Mention leftMention();

  @Nullable
  public abstract Mention rightMention();

  public abstract Optional<ValueMention> timeArg();

  public abstract Tense tense();

  public abstract Modality modality();

  public abstract Optional<Symbol> timeRole();

  public abstract double score();

  public abstract Optional<String> pattern();

  public abstract Optional<String> model();

  @Override
  public abstract Optional<Symbol> externalID();

  @org.immutables.value.Value.Check
  public void check() {
    checkArgument(timeArg().isPresent() == timeRole().isPresent());
  }

  /* The Span on a RelationMention is the smallest span containing its left
   * argument, right argument, and, if present, time argument.
   */
  @Override
  public final Span span() {
    final List<Span> spans = Lists.newArrayList();

    spans.add(leftMention().span());
    spans.add(rightMention().span());
    if (timeArg().isPresent()) {
      spans.add(timeArg().get().span());
    }

    return TokenSequence.union(spans);
  }

  @Override
  public final TokenSpan tokenSpan() {
    return span();
  }

  /**
   * @deprecated Provided for Serif compatibility.
   */
  @Deprecated
  public final Optional<Symbol> roleForMention(Mention m) {
    if (leftMention() == m) {
      return Optional.of(ARG1);
    } else if (rightMention() == m) {
      return Optional.of(ARG2);
    } else {
      return Optional.absent();
    }
  }

  /**
   * @deprecated Provided for Serif compatibility
   */
  @Deprecated
  public Optional<Symbol> roleForValueMention(ValueMention vm) {
    if (timeArg().isPresent() && vm == timeArg().get()) {
      return Optional.of(timeRole().get());
    } else {
      return Optional.absent();
    }
  }

  public static class Builder extends ImmutableRelationMention.Builder {

  }
}
