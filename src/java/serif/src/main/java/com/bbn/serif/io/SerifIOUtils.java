package com.bbn.serif.io;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.WithDocTheory;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public final class SerifIOUtils {

  private SerifIOUtils() {
    throw new UnsupportedOperationException();
  }


  /**
   * Loads SerifXML documents from a file list into memory, returning a map from the docids to the
   * DocTheories.  Use this only if you are sure your file list is small, since it loads all
   * documents into memory.
   * @deprecated If you want to use this, make something which doesn't take its settings from
   * params.
   */
  @Deprecated
  public static Map<String, DocTheory> makeDocIDsToDocs(
      final Parameters serifParams, final List<File> inputFiles) throws IOException {
    final SerifXMLLoader loader = SerifXMLLoader.createFrom(serifParams);
    final ImmutableMap.Builder<String, DocTheory> docidsToDocsBuilder =
        ImmutableMap.builder();
    for (final File f : inputFiles) {
      final DocTheory dt = loader.loadFrom(f);

      docidsToDocsBuilder.put(dt.document().name().toString(), dt);
    }

    final Map<String, DocTheory> docidsToDocs = docidsToDocsBuilder.build();
    return docidsToDocs;
  }

  /**
   * Given an Iterable of Files and a DocTheory loader, makes an Iterable which loads them lazily
   * using that loader. Note that because of the Iterable interface, this swallows the checked
   * IOException from loading a file and wraps it in a RuntimeException.
   */
  public static Iterable<DocTheory> docTheoriesFromFiles(final Iterable<File> files,
      final DocTheoryLoader loader) {
    return transform(files, new Function<File, DocTheory>() {
      @Override
      public DocTheory apply(final File f) {
        try {
          return loader.loadFrom(Files.asCharSource(f, Charsets.UTF_8));
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  /**
   * Given an Iterable of CharSources and a DocTheory loader, makes an Iterable which loads them
   * lazily using that loader. Note that because of the Iterable interface, this swallows the
   * checked IOException from loading a CharSource and wraps it in a RuntimeException.
   */
  public static Iterable<DocTheory> docTheoriesFromCharSources(final Iterable<CharSource> sources,
      final DocTheoryLoader loader) {
    return transform(sources, new Function<CharSource, DocTheory>() {
      @Override
      public DocTheory apply(final CharSource s) {
        try {
          return loader.loadFrom(s);
        } catch (final IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  /**
   * Returns an {@link java.lang.Iterable} over the sentence theories in the supplied SerifXML
   * files, which will be loaded using the supplied {@link com.bbn.serif.io.SerifXMLLoader}. The
   * order of the sentence theories will be the same as in the SerifXML files; the ordering of files
   * will be the same as {@code files}.  Empty sentences will be skipped.
   */
  public static Iterable<WithDocTheory<SentenceTheory>> sentenceTheoriesFromFiles(
      final Iterable<File> files,
      final SerifXMLLoader loader) {
    return nonEmptySentenceTheoriesFromFiles(files, loader, Functions.<DocTheory>identity());
  }

  /**
   * You should almost always prefer {@code nonEmptySentenceTheoriesFromFiles} because empty
   * sentences can cause crashes.
   */
  @Deprecated
  public static Iterable<WithDocTheory<SentenceTheory>> sentenceTheoriesFromFiles(
      final Iterable<File> files,
      final SerifXMLLoader loader, Function<DocTheory, DocTheory> transformation) {
    final Function<DocTheory, List<WithDocTheory<SentenceTheory>>> GetSentenceTheories =
        new Function<DocTheory, List<WithDocTheory<SentenceTheory>>>() {
          @Override
          public List<WithDocTheory<SentenceTheory>> apply(final DocTheory dt) {
            return dt.sentenceTheoriesWithDocTheory();
          }
        };

    return concat(
        transform(
            transform(
                docTheoriesFromFiles(files, loader),
                transformation),
            GetSentenceTheories));
  }


  public static Iterable<WithDocTheory<SentenceTheory>> nonEmptySentenceTheoriesFromFiles(
      final Iterable<File> files,
      final SerifXMLLoader loader, Function<DocTheory, DocTheory> transformation) {
    final Function<DocTheory, Iterable<WithDocTheory<SentenceTheory>>> GetSentenceTheories =
        new Function<DocTheory, Iterable<WithDocTheory<SentenceTheory>>>() {
          @Override
          public Iterable<WithDocTheory<SentenceTheory>> apply(final DocTheory dt) {
            return dt.nonEmptySentenceTheoriesWithDocTheory();
          }
        };

    return concat(
        transform(
            transform(
                docTheoriesFromFiles(files, loader),
                transformation),
            GetSentenceTheories));
  }

  public static Iterable<WithDocTheory<SentenceTheory>> nonEmptySentenceTheoriesFromFiles(
      final Iterable<File> files, final SerifXMLLoader loader) {
    return nonEmptySentenceTheoriesFromFiles(files, loader, Functions.<DocTheory>identity());
  }
}
