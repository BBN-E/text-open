package com.bbn.serif.morphology;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.LexicalForm;
import com.bbn.serif.theories.MorphFeature;
import com.bbn.serif.theories.MorphToken;
import com.bbn.serif.theories.MorphTokenAnalysis;
import com.bbn.serif.theories.MorphTokenSequence;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.bbn.bue.common.StringUtils.unicodeFriendly;

/**
 * Static utilities related to morphological analysis.
 */
public final class Morphology {

  private Morphology() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns true if and only if there is some bit of morphological analysis somewhere in
   * the document.
   */
  public boolean documentContainsMorphologicalInformation(DocTheory dt) {
    for (final SentenceTheory sentenceTheory : dt.sentenceTheories()) {
      if (!sentenceTheory.morphTokenSequences().isEmpty()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the given token either (a) is itself in the given set or
   * (b) has a lemma in the given lemma set for any available analysis
   * on the {@link SentenceTheory}.
   */
  public static boolean hasWordOrLemmaInSetInAnyAnalysis(SentenceTheory st, Token tok,
      Set<Symbol> lemmaSet) {
    return lemmaSet.contains(tok.symbol()) || hasLemmaInSetInAnyAnalysis(st, tok, lemmaSet);
  }

  /**
   * Checks if the given token has a lemma in the given lemma set for any available analysis
   * on the {@link SentenceTheory}.
   *
   * Note this will not succeed simply because the word itself is in the set.  If you want that
   * behavior, use {@link #hasWordOrLemmaInSetInAnyAnalysis(SentenceTheory, Token, Set)}
   */
  public static boolean hasLemmaInSetInAnyAnalysis(SentenceTheory st, Token tok,
      Set<Symbol> lemmaSet) {
    for (final MorphTokenSequence morphTokenSequence : st.morphTokenSequences()) {
      final Optional<MorphToken> forToken = morphTokenSequence.morphTokenForToken(tok);
      if (forToken.isPresent()) {
        for (final MorphTokenAnalysis analysis : forToken.get().analyses()) {
          for (final LexicalForm lexicalForm : analysis.lemmas()) {
            if (lemmaSet.contains(lexicalForm.form())) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks if {@code feature} is present as a morphological feature in any available analysis of
   * the {@code tok}.
   */
  public static boolean hasFeatureInAnyAnalysis(SentenceTheory st, Token tok,
      MorphFeature feature) {
    for (final MorphTokenSequence morphTokenSequence : st.morphTokenSequences()) {
      final Optional<MorphToken> forToken = morphTokenSequence.morphTokenForToken(tok);
      if (forToken.isPresent()) {
        for (final MorphTokenAnalysis analysis : forToken.get().analyses()) {
          if (analysis.morphFeatures().contains(feature)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Checks if any of {@code features} is present as a morphological feature in any available
   * analysis of {@code tok}
   */
  public static boolean hasAnyFeatureInAnyAnalysis(SentenceTheory st, Token tok,
      Set<MorphFeature> features) {
    for (final MorphTokenSequence morphTokenSequence : st.morphTokenSequences()) {
      final Optional<MorphToken> forToken = morphTokenSequence.morphTokenForToken(tok);
      if (forToken.isPresent()) {
        for (final MorphTokenAnalysis analysis : forToken.get().analyses()) {
          if (!Sets.intersection(analysis.morphFeatures(), features).isEmpty()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Returns if the lemma for this word is marked as having been in the morphological analyzer's
   * lexicon for any available analysis.
   */
  public static boolean markedAsInLexiconInAnyAnalysis(final SentenceTheory st, final Token tok) {
    for (final MorphTokenSequence morphTokenSequence : st.morphTokenSequences()) {
      final Optional<MorphToken> forToken = morphTokenSequence.morphTokenForToken(tok);
      if (forToken.isPresent()) {
        for (final MorphTokenAnalysis analysis : forToken.get().analyses()) {
          for (final LexicalForm lemma : analysis.lemmas()) {
            if (lemma.inLexicon()) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  /**
   * Predicate which applies {@link #hasWordOrLemmaInSetInAnyAnalysis(SentenceTheory, Token, Set)}.
   */
  public static Predicate<Token> hasWordOrLemmaInSetInAnyAnalysisPredicate(
      final SentenceTheory st, final Iterable<Symbol> lemmas) {
    return new HasWordOrLemmaInSetInAnyAnalysisPredicate(st, lemmas);
  }

  /**
   * For each possible analysis of the given name into per-token lemmas, returns the
   * cartesian product of these lemmas separated by single spaces.  If
   * {@code alwaysIncludeOriginalForms} is {@code true}, the name's own tokenized text
   * is always included as a possibility.  Note that if it is {@code false}, this may
   * return the empty set if no morphological analyses are available for these tokens.
   */
  public static ImmutableSet<UnicodeFriendlyString> lemmatizedText(
      final SentenceTheory st, final Name name, boolean alwaysIncludeOriginalForms,
      boolean restrictToLexiconAnalyses) {
    final ImmutableSet.Builder<UnicodeFriendlyString> ret = ImmutableSet.builder();

    if (alwaysIncludeOriginalForms) {
      ret.add(name.span().tokenizedText());
    }

    for (final MorphTokenSequence mts : st.morphTokenSequences()) {
      ret.addAll(lemmatizedText(mts, name, alwaysIncludeOriginalForms, restrictToLexiconAnalyses));
    }

    return ret.build();
  }

  private static ImmutableSet<UnicodeFriendlyString> lemmatizedText(
      final MorphTokenSequence morphTokSeq, final Name name, boolean alwaysIncludeOriginalForms,
      boolean restrictToLexiconAnalyses) {
    final List<Set<String>> possibilitiesForEachToken = new ArrayList<>();

    for (final Token token : name.span()) {
      final ImmutableSet.Builder<String> possibilitiesForThisToken = ImmutableSet.builder();

      final Optional<MorphToken> optMorphToken = morphTokSeq.morphTokenForToken(token);
      if (optMorphToken.isPresent()) {
        for (final MorphTokenAnalysis analysis : optMorphToken.get().analyses()) {
          for (final LexicalForm lexicalForm : analysis.lemmas()) {
            if (lexicalForm.inLexicon() || !restrictToLexiconAnalyses) {
              possibilitiesForThisToken.add(lexicalForm.form().asString());
            }
          }
        }
      }

      if (alwaysIncludeOriginalForms) {
        possibilitiesForThisToken.add(token.symbol().asString());
      }
      possibilitiesForEachToken.add(possibilitiesForThisToken.build());
    }

    final ImmutableSet.Builder<UnicodeFriendlyString> ret = ImmutableSet.builder();

    for (final List<String> possibleCombination : Sets
        .cartesianProduct(possibilitiesForEachToken)) {
      ret.add(unicodeFriendly(StringUtils.spaceJoiner().join(possibleCombination)));
    }

    return ret.build();
  }

  private static class HasWordOrLemmaInSetInAnyAnalysisPredicate implements Predicate<Token> {

    private final SentenceTheory st;
    private final ImmutableSet<Symbol> lemmas;

    HasWordOrLemmaInSetInAnyAnalysisPredicate(final SentenceTheory st,
        final Iterable<Symbol> lemmas) {
      this.st = st;
      this.lemmas = ImmutableSet.copyOf(lemmas);
    }

    @Override
    public boolean apply(final Token tok) {
      return hasWordOrLemmaInSetInAnyAnalysis(st, tok, lemmas);
    }
  }
}
