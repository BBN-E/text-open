package com.bbn.serif.patterns.matching;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.patterns.PatternSet;
import com.bbn.serif.patterns.Pattern;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMultimap;

import org.immutables.value.Value;

import java.util.List;

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
    if (!patternSet().getEntityLabels().isEmpty()) {
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

    // TODO: return all matches, not just the toplevel match
    public final PatternReturns findMatchesInDocument() {
      final PatternReturns.Builder ret = new PatternReturns.Builder();
      for (final SentenceTheory st : docTheory().sentenceTheories()) {
        ret.addAll(findMatchesIn(st));
      }
      final PatternMatchState matchState = PatternMatchState.create();
      for (final Pattern pattern : patternSet().getTopLevelPatterns()) {
        if (pattern instanceof EventEventRelationMatchingPattern) {
          for (final EventEventRelationMention eer : docTheory().eventEventRelationMentions()) {
//            System.out.println("Attempting to match " + eer.toString());
            ret.addAll(((EventEventRelationMatchingPattern) pattern).match(docTheory(), eer, matchState, true));
          }
        } else {
          // should have already handled all these cases at the sentence-level
        }
      }
      return ret.build();
    }

    public final PatternReturns findMatchesIn(SentenceTheory st) {
      final PatternMatchState matchState = PatternMatchState.create();
      final PatternReturns.Builder ret = new PatternReturns.Builder();

      for (final Pattern pattern : patternSet().getTopLevelPatterns()) {
        if (pattern instanceof MentionMatchingPattern) {
          for (final Mention mention : st.mentions()) {
            ret.addAll(((MentionMatchingPattern) pattern).match(docTheory(), st, mention, matchState, true));
          }
        } else if (pattern instanceof PropMatchingPattern) {
          for (final Proposition proposition : st.propositions()) {
            ret.addAll(((PropMatchingPattern) pattern).match(docTheory(), st, proposition, matchState, true));
          }
          for (final Proposition proposition : st.dependencies()) {
            ret.addAll(((PropMatchingPattern) pattern).match(docTheory(), st, proposition, matchState, true));
          }
        } else if (pattern instanceof EventMatchingPattern) {
          for (final EventMention em : st.eventMentions()) {
            ret.addAll(((EventMatchingPattern) pattern).match(docTheory(), st, em, matchState, true));
          }
        } else if (pattern instanceof EventEventRelationMatchingPattern) {
          // Handle these at the document level
        } else if (pattern instanceof SentenceMatchingPattern) {
          ret.addAll(((SentenceMatchingPattern) pattern).match(docTheory(), st, matchState, true));
        }
        else {
          throw new UnsupportedOperationException("Unsupported pattern type " + pattern);
        }

      }

      return ret.build();
    }
  }

  public static class Builder extends ImmutablePatternSetMatcher.Builder {

  }
}

