package com.bbn.serif.theories.diff;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.List;

/**
 * The difference between two {@link DocTheory}s. Currently only aligns the sentence theories by
 * sentence index and compares them.
 */
@Beta
@Value.Immutable
@Functional
@TextGroupImmutable
abstract class DocTheoryDiff implements Difference<DocTheory>, WithDocTheoryDiff {
  @Value.Parameter
  @Override
  public abstract Optional<DocTheory> left();
  @Value.Parameter
  @Override
  public abstract Optional<DocTheory> right();
  public abstract Optional<CollectionDifference<List<SentenceTheory>, SentenceTheory, SentenceTheoryDiff>>
    sentenceTheoryDiffs();

  @Override
  public void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    if (sentenceTheoryDiffs().isPresent()) {
      sb.append(Strings.repeat(" ", indentSpaces))
          .append("sentenceTheories: ");
      sentenceTheoryDiffs().get().writeTextReport(sb, indentSpaces+2);
    }
  }

  public static class Builder extends ImmutableDocTheoryDiff.Builder {}
}


