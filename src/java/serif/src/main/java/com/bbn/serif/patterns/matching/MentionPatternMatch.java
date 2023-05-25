package com.bbn.serif.patterns.matching;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.patterns.Pattern;
import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A match resulting from a {@link MentionMatchingPattern}.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class MentionPatternMatch extends AbstractPatternMatch implements PatternMatch  {

  @Override
  public abstract DocTheory docTheory();

  @Override
  public abstract Optional<Pattern> pattern();

  @Override
  public abstract Optional<SentenceTheory> sentenceTheory();

  public abstract Mention mention();

  @Override
  public final Optional<Spanning> spanning() {
    return Optional.<Spanning>of(mention());
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  public final SentenceTheory guaranteedSentenceTheory() {
    return sentenceTheory().get();
  }

  public final Spanning guaranteedSpanning() {
    return mention();
  }

  @Override
  @Value.Check
  protected void check() {
    super.check();
    checkArgument(sentenceTheory().isPresent());
  }

  public static MentionPatternMatch of(Pattern pattern, DocTheory docTheory,
      SentenceTheory sentenceTheory, Mention mention) {
    return ImmutableMentionPatternMatch.builder()
        .pattern(pattern)
        .docTheory(docTheory)
        .sentenceTheory(sentenceTheory)
        .mention(mention).build();
  }
}


