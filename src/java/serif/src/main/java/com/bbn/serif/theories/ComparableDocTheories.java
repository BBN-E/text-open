package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.ComparableDocTheoriesRef;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.io.cache.DocTheoryCache;
import com.bbn.serif.languages.SerifLanguage;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents two groups of documents in two different languages which we believe to be comparable
 * for some reason.
 *
 * When one language is English or "higher-resource", we place it on the left by convention.
 */
@TextGroupImmutable
@Value.Immutable
@Functional
public abstract class ComparableDocTheories implements WithComparableDocTheories {

  @Value.Parameter
  public abstract ImmutableSet<DocTheory> leftLanguageDocs();

  @Value.Parameter
  public abstract ImmutableSet<DocTheory> rightLanguageDocs();

  @Value.Check
  protected void check() {
    checkAllLanguagesMatch(leftLanguageDocs());
    checkAllLanguagesMatch(rightLanguageDocs());
  }

  private void checkAllLanguagesMatch(final ImmutableSet<DocTheory> docTheories) {
    checkArgument(!docTheories.isEmpty());
    final SerifLanguage lang = docTheories.asList().get(0).document().language();
    for (final DocTheory dt : docTheories) {
      if (!lang.equals(dt.document().language())) {
        throw new SerifException("Conflicting languages on same side of comparable document group: "
            + lang + ", " + dt.document().language());
      }
    }
  }

  /**
   * Function which can be used to lazily turn {@link ComparableDocTheoriesRef} into {@link
   * ComparableDocTheories} using {@link com.google.common.collect.Iterables#transform(Iterable,
   * Function)}.
   */
  public static Function<ComparableDocTheoriesRef, ComparableDocTheories> resolve(
      DocTheoryCache leftDocs, DocTheoryCache rightDocs) {
    return new ResolveFunction(leftDocs, rightDocs);
  }

  private static final class ResolveFunction
      implements Function<ComparableDocTheoriesRef, ComparableDocTheories> {

    private final DocTheoryCache leftDocCache;
    private final DocTheoryCache rightDocCache;

    ResolveFunction(final DocTheoryCache leftDocCache,
        final DocTheoryCache rightDocCache) {
      this.leftDocCache = checkNotNull(leftDocCache);
      this.rightDocCache = checkNotNull(rightDocCache);
    }

    @Override
    public ComparableDocTheories apply(final ComparableDocTheoriesRef input) {
      final ImmutableSet.Builder<DocTheory> leftDocs = ImmutableSet.builder();
      final ImmutableSet.Builder<DocTheory> rightDocs = ImmutableSet.builder();

      try {
        for (final Symbol leftDoc : input.leftLanguageDocIds()) {
          leftDocs.add(leftDocCache.getDocTheory(leftDoc));
        }
        for (final Symbol rightDoc : input.rightLanguageDocIds()) {
          rightDocs.add(rightDocCache.getDocTheory(rightDoc));
        }
      } catch (IOException ioe) {
        throw new SerifException(ioe);
      }

      return new ComparableDocTheories.Builder().leftLanguageDocs(leftDocs.build())
        .rightLanguageDocs(rightDocs.build()).build();
    }
  }

  public static class Builder extends ImmutableComparableDocTheories.Builder {}
}

