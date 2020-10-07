package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@TextGroupImmutable
@Value.Immutable
public abstract class ValueMentions
    implements Iterable<ValueMention>, PotentiallyAbsentSerifTheory {

  public abstract ImmutableSet<ValueMention> valueMentions();

  @Override
  @Value.Default
  public boolean isAbsent() {
    return false;
  }

  private static final ValueMentions ABSENT = new ValueMentions.Builder().isAbsent(true).build();


  public final int numValueMentions() {
    return valueMentions().size();
  }

  public final ValueMention valueMention(int idx) {
    return valueMentions().asList().get(idx);
  }

  public final int size() {
    return numValueMentions();
  }

  public final ValueMention get(int idx) {
    return valueMention(idx);
  }

  public List<ValueMention> asList() {
    return valueMentions().asList();
  }

  @Override
  public Iterator<ValueMention> iterator() {
    return valueMentions().iterator();
  }

  public static ValueMentions create(Iterable<ValueMention> valueMentions) {
    return new ValueMentions.Builder().valueMentions(valueMentions).build();
  }

  public static ValueMentions createEmpty() {
    return new ValueMentions.Builder().build();
  }


  public static ValueMentions absent() {
    return ABSENT;
  }

  @Value.Check
  protected void check() {
    checkArgument(!isAbsent() || valueMentions().isEmpty());
  }

  public final Optional<ValueMention> longestValueMentionBeginningAt(Token tok) {
    ValueMention bestVM = null;
    int bestSize = 0;

    for (final ValueMention vm : this) {
      if (vm.span().startIndex() == tok.index()) {
        if (vm.span().size() > bestSize) {
          bestVM = vm;
          bestSize = vm.span().size();
        }
      }
    }
    return Optional.fromNullable(bestVM);
  }

  /**
   * Returns the value mention whose span matches the provided one, if any. If there is more than
   * one such value mention, the first in iteration order is returned.
   */
  public Optional<ValueMention> lookupByTokenSpan(TokenSequence.Span span) {
    return lookupByTokenSpan(asList(), span);
  }

  public static Optional<ValueMention> lookupByTokenSpan(final Iterable<ValueMention> valueMentions,
      final TokenSequence.Span span) {
    for (final ValueMention vm : valueMentions) {
      if (vm.span().equals(span)) {
        return Optional.of(vm);
      }
    }
    return Optional.absent();
  }

  public static class Builder extends ImmutableValueMentions.Builder {

  }
}
