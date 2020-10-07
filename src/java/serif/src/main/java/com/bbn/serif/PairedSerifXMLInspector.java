package com.bbn.serif;

import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.Inspector;
import com.bbn.bue.common.evaluation.EvalPair;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.intersection;

/**
 * Utility to inspect paired up SerifXML files.
 */
public final class PairedSerifXMLInspector {

  private static final Logger log = LoggerFactory.getLogger(PairedSerifXMLInspector.class);

  private final SerifXMLLoader loader;

  @Inject
  private PairedSerifXMLInspector(final SerifXMLLoader loader) {
    this.loader = checkNotNull(loader);
  }

  /**
   * @deprecated Prefer {@link #createForLoader(SerifXMLLoader)}
   */
  @Deprecated
  public static PairedSerifXMLInspector create() throws IOException {
    return new PairedSerifXMLInspector(SerifXMLLoader.builder().build());
  }

  public static PairedSerifXMLInspector createForLoader(final SerifXMLLoader loader) {
    return new PairedSerifXMLInspector(loader);
  }

  /**
   * Applies supplied inspectors to the paired files.  If any of {@code docIDsToScore} are missing
   * from the key, a {@link SerifException} is thrown. Otherwise, a warning is printed for any
   * docIDs in only the key or the test. Finally, for all docIDs in both, the corresponding SerifXML
   * files are loading into an {@link EvalPair} and fed to each of the provided inspectors.
   */
  public void inspectPairedFiles(final Set<Symbol> docIDsToScore, final Map<Symbol, File> keyFiles,
      final Map<Symbol, File> testFiles,
      final Iterable<? extends Inspector<EvalPair<Optional<DocTheory>, Optional<DocTheory>>>> corpusObservers
  ) throws IOException {
    warnOfMissingFiles(docIDsToScore, keyFiles, testFiles);

    for (final Symbol docID : docIDsToScore) {
      log.info("Scoring {}", docID);
      // we know this won't fail by first check in warnOfMissingFiles
      final Optional<DocTheory> keyDocTheory = Optional.of(loader.loadFrom(keyFiles.get(docID)));
      final Optional<DocTheory> testDocTheory;
      if (testFiles.containsKey(docID)) {
        testDocTheory = Optional.of(loader.loadFrom(testFiles.get(docID)));
      } else {
        testDocTheory = Optional.absent();
      }
      for (final Inspector<EvalPair<Optional<DocTheory>, Optional<DocTheory>>> inspector : corpusObservers) {
        inspector.inspect(EvalPair.of(keyDocTheory, testDocTheory));
      }
    }
    for (final Finishable observer : corpusObservers) {
      observer.finish();
    }
  }

  private static void warnOfMissingFiles(final Set<Symbol> docIDsToScore,
      final Map<Symbol, File> keyDocIdToFileMap, final Map<Symbol, File> testDocIdToFileMap) {
    final Set<Symbol> missingFromKey = difference(docIDsToScore, keyDocIdToFileMap.keySet());
    if (!missingFromKey.isEmpty()) {
      throw new SerifException("Scoring was requested for the following document IDs, but they "
          + "were missing from the key: " + missingFromKey);
    }

    final ImmutableSet<Symbol>
        docIDsInKeyOnly = intersection(
        difference(keyDocIdToFileMap.keySet(), testDocIdToFileMap.keySet()),
        docIDsToScore).immutableCopy();
    if (!docIDsInKeyOnly.isEmpty()) {
      log.warn("The following document IDs appear only in the key: {}", docIDsInKeyOnly);
    }

    final ImmutableSet<Symbol> docIDsInTestOnly = intersection(
        difference(testDocIdToFileMap.keySet(), keyDocIdToFileMap.keySet()),
        docIDsToScore).immutableCopy();
    if (!docIDsInTestOnly.isEmpty()) {
      log.warn("The following document IDs appear only in the test data: {}", docIDsInTestOnly);
    }
  }


}
