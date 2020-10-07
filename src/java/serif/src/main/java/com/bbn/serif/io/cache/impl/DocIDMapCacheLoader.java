package com.bbn.serif.io.cache.impl;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.io.DocIDToFileMapping;
import com.bbn.nlp.io.DocIDToFileMappings;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A {@link CacheLoader} capable of taking a document ID and returning the corresponding {@link
 * DocTheory} on command according to a supplied mapping of document IDs to SerifXML files.
 *
 * If the supplied docid cannot be found, an {@link IOException} is thrown.
 *
 * This can be used to easily built a cache:
 *
 * {@code LoadingCache<Symbol, DocTheory> myCache = CacheBuilder .newBuilder() .maximumSize(50)
 * .build(DocIDMapCacheLoader.from(myIDMap, myLoader) }
 *
 * @author rgabbard
 */
final class DocIDMapCacheLoader extends CacheLoader<Symbol, DocTheory> {

  private final SerifXMLLoader loader;
  private final DocIDToFileMapping docidMap;

  private DocIDMapCacheLoader(DocIDToFileMapping docidMap, final SerifXMLLoader loader) {
    this.docidMap = checkNotNull(docidMap);
    this.loader = checkNotNull(loader);
  }

  /**
   * @deprecated Prefer {@link #from(com.bbn.nlp.io.DocIDToFileMapping,
   * com.bbn.serif.io.SerifXMLLoader)}
   */
  @Deprecated
  public static DocIDMapCacheLoader from(final Map<Symbol, File> docidMap,
      final SerifXMLLoader loader) {
    return new DocIDMapCacheLoader(DocIDToFileMappings.forMap(docidMap), loader);
  }


  public static DocIDMapCacheLoader from(final DocIDToFileMapping docidMap,
      final SerifXMLLoader loader) {
    return new DocIDMapCacheLoader(docidMap, loader);
  }


  @Override
  public DocTheory load(final Symbol key) throws Exception {
    final Optional<File> f = docidMap.fileForDocID(key);

    if (f.isPresent()) {
      return loader.loadFrom(f.get());
    } else {
      throw new IOException(String.format("No SerifXMLfile known for doc ID %s", key));
    }
  }

}
