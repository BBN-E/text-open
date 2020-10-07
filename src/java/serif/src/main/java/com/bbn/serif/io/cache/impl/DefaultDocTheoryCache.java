package com.bbn.serif.io.cache.impl;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.io.DocIDToFileMapping;
import com.bbn.nlp.io.DocIDToFileMappings;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.cache.DocTheoryCache;
import com.bbn.serif.theories.DocTheory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of {@link com.bbn.serif.io.cache.DocTheoryCache}, which loads lazily and
 * caches.
 *
 * @author rgabbard
 */
public final class DefaultDocTheoryCache implements DocTheoryCache {

  private DefaultDocTheoryCache(final LoadingCache<Symbol, DocTheory> innerCache) {
    this.innerCache = checkNotNull(innerCache);
  }

  /**
   * Gets a document theory for the specified document ID. Throws an {@link IOException} if there is
   * no theory available for the specified document ID.
   */
  @Override
  public DocTheory getDocTheory(final Symbol docid) throws IOException {
    try {
      return innerCache.get(docid);
    } catch (final ExecutionException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  public static Builder createFromDocIdMap(final DocIDToFileMapping docidToSerifXML) {
    return Builder.fromDocIdMap(docidToSerifXML);
  }

  /**
   * Creates a cache builder from a map from document IDs to SerifXML files.
   *
   * @deprecated Prefer {@link #createFromDocIdMap(com.bbn.nlp.io.DocIDToFileMapping)}
   */
  @Deprecated
  public static Builder createFromDocIdMap(final Map<Symbol, File> docidToSerifXML) {
    return Builder.fromDocIdMap(DocIDToFileMappings.forMap(docidToSerifXML));
  }

  public static class Builder {

    private Builder() {
    }

    public DocTheoryCache build() throws IOException {
      return new DefaultDocTheoryCache(
          CacheBuilder.newBuilder()
              .maximumSize(maxElements >= 0 ? maxElements : 10)
              .<Symbol, DocTheory>build(DocIDMapCacheLoader.from(
                  docidMap,
                  loader != null ? loader : SerifXMLLoader.builder().build())));
    }

    private static Builder fromDocIdMap(final DocIDToFileMapping docidToSerifXML) {
      final Builder ret = new Builder();
      ret.docidMap = docidToSerifXML;
      return ret;
    }

    /**
     * Sets a maximum size for the cache. This is only a suggestion and not strictly enforced.
     */
    public Builder setMaxSize(final int maxSize) {
      checkArgument(maxSize >= 0);
      maxElements = maxSize;
      return this;
    }

    /**
     * If you do not wish to use the default {@code SerifXMLLoader.fromStandardACETypes} loader, you
     * need to specify it with this.
     */
    public Builder setLoader(final SerifXMLLoader loader) {
      this.loader = checkNotNull(loader);
      return this;
    }


    private SerifXMLLoader loader = null;
    private DocIDToFileMapping docidMap = null;
    private int maxElements = -1;
  }

  private final LoadingCache<Symbol, DocTheory> innerCache;
}
