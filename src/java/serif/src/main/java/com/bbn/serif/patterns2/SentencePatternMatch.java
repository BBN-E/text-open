package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A match of a {@link Pattern} against a whole {@link SentenceTheory}
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class SentencePatternMatch extends AbstractPatternMatch implements PatternMatch  {

  @Override
  public abstract DocTheory docTheory();

  @Override
  public abstract Optional<Pattern> pattern();

  @Override
  public abstract Optional<SentenceTheory> sentenceTheory();

  @Override
  public final Optional<Spanning> spanning() {
    return Optional.<Spanning>of(guaranteedSentenceTheory());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public final SentenceTheory guaranteedSentenceTheory() {
    return sentenceTheory().get();
  }

  public final Spanning guaranteedSpanning() {
    return guaranteedSentenceTheory();
  }

  @Override
  @Value.Check
  protected void check() {
    super.check();
    checkArgument(sentenceTheory().isPresent());
  }

  public static SentencePatternMatch of(Pattern pattern, DocTheory docTheory,
      SentenceTheory sentenceTheory) {
    return ImmutableSentencePatternMatch.builder()
        .pattern(pattern)
        .docTheory(docTheory)
        .sentenceTheory(sentenceTheory)
        .build();
  }

  @Override
  public String toString() {
    return "Match[dt=" + docTheory().document().name() + ";st="
        + guaranteedSentenceTheory().span().tokenizedText() +"]";
  }
}
