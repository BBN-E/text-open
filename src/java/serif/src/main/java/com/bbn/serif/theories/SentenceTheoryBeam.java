package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@JsonDeserialize
@JsonSerialize
@Functional
@TextGroupImmutable
@Value.Immutable
public abstract class SentenceTheoryBeam implements Iterable<SentenceTheory>, WithSentenceTheoryBeam {
  public abstract Sentence sentence();
  public abstract ImmutableList<SentenceTheory> sentenceTheories();

  @Value.Derived
  public Optional<SentenceTheory> primaryTheory() {
    if (!sentenceTheories().isEmpty()) {
      return Optional.of(sentenceTheories().get(0));
    } else {
      return Optional.absent();
    }
  }

  @Value.Check
  protected void check() {
    for (final SentenceTheory sentenceTheory : sentenceTheories()) {
      checkArgument(sentence().sentenceNumber() == sentenceTheory.sentenceNumber(), "All sentence theories in "
              + "a SentenceTheoryBeam must share the same index, but saw %s and %s in the same beam",
          sentence().sentenceNumber(), sentenceTheory.sentenceNumber());
      checkArgument(sentenceTheory.sentence().equals(sentence()));
    }
  }

  public static SentenceTheoryBeam forSentenceTheories(final Sentence sentence,
      final List<SentenceTheory> sentenceTheories) {
    return new SentenceTheoryBeam.Builder().sentence(sentence).sentenceTheories(sentenceTheories).build();
  }

  public static SentenceTheoryBeam forSentenceTheory(final SentenceTheory sentenceTheory) {
    return new SentenceTheoryBeam.Builder().sentence(sentenceTheory.sentence())
      .sentenceTheories( ImmutableList.of(sentenceTheory)).build();
  }

  public final int numSentenceTheories() {
    return sentenceTheories().size();
  }


  public final SentenceTheory nthBestSentenceTheory(int idx) {
    return sentenceTheories().get(idx);
  }

  /**
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public final Iterator<SentenceTheory> iterator() {
    return sentenceTheories().iterator();
  }


  /**
   * Replaces the primary sentence theory of this beam with the given sentence theory. If this
   * beam had been previously empty, the given sentence theory is added as the primary theory.
   */
  public SentenceTheoryBeam copyWithPrimaryTheoryReplaced(final SentenceTheory st) {
    final ImmutableList.Builder<SentenceTheory> newTheories = ImmutableList.builder();

    newTheories.add(st);
    for (final SentenceTheory theory : Iterables.skip(sentenceTheories(), 1)) {
      newTheories.add(theory);
    }

    return this.withSentenceTheories(newTheories.build());
  }

  public static class Builder extends ImmutableSentenceTheoryBeam.Builder {}
}
