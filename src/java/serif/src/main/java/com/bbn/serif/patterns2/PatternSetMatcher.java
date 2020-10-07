package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMultimap;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Matches a {@link PatternSet} against a {@link DocTheory}.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
@Value.Enclosing
public abstract class PatternSetMatcher {

  public abstract PatternSet patternSet();

  public DocumentPatternMatcher inContextOf(DocTheory dt) {
    if (!patternSet().entityPatterns().isEmpty()) {
      throw new UnsupportedOperationException("Entity label patterns not yet supported");
    }

    return new DocumentPatternMatcher(dt, ImmutableMultimap.<Entity, Symbol>of());
  }

  public static PatternSetMatcher of(PatternSet patternSet) {
    return new PatternSetMatcher.Builder().patternSet(patternSet).build();
  }

  public final class DocumentPatternMatcher {

    private final DocTheory docTheory;
    private final ImmutableMultimap<Entity, Symbol> entityLabels;

    private DocumentPatternMatcher(final DocTheory docTheory,
        final ImmutableMultimap<Entity, Symbol> entityLabels) {
      this.docTheory = checkNotNull(docTheory);
      this.entityLabels = checkNotNull(entityLabels);
    }

    public final DocTheory docTheory() {
      return docTheory;
    }

    private ImmutableMultimap<Entity, Symbol> entityLabels() {
      return entityLabels;
    }

    public final PatternReturns findMatchesInDocument() {
      final PatternReturns.Builder ret = new PatternReturns.Builder();
      for (final SentenceTheory st : docTheory().sentenceTheories()) {
        ret.addAll(findMatchesIn(st));
      }
      return ret.build();
    }

    public final PatternReturns findMatchesIn(SentenceTheory st) {
      final PatternMatchState matchState = PatternMatchState.create();
      final PatternReturns.Builder ret = new PatternReturns.Builder();

      for (final Pattern pattern : patternSet().patterns()) {
        if (pattern instanceof MentionMatchingPattern) {
          for (final Mention mention : st.mentions()) {
            ret.addAll(((MentionMatchingPattern) pattern).match(docTheory(), st, mention, matchState, true));
          }
        } else {
          throw new UnsupportedOperationException("Unsupported pattern type " + pattern);
        }

        if (pattern instanceof SentenceMatchingPattern) {
          ret.addAll(((SentenceMatchingPattern) pattern).match(docTheory(), st, matchState));
        }
      }

      return ret.build();
    }
  }

  public static class Builder extends ImmutablePatternSetMatcher.Builder {

  }
}

