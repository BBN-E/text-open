package com.bbn.serif.io.cache.impl;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.cache.DocTheoryCache;
import com.bbn.serif.theories.DocTheory;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

/**
 * A mock {@link com.bbn.serif.io.cache.DocTheoryCache} for unit tests.
 */
public final class InMemoryDocTheoryCache implements DocTheoryCache {

  private final Map<Symbol, DocTheory> map;

  private InMemoryDocTheoryCache(Map<Symbol, DocTheory> map) {
    this.map = ImmutableMap.copyOf(map);
  }

  @Override
  public DocTheory getDocTheory(final Symbol docid) throws IOException {
    final DocTheory ret = map.get(docid);
    if (ret != null) {
      return ret;
    } else {
      throw new IOException(String.format("Cannot load document %s", docid));
    }
  }

  public static DocTheoryCache fromMap(final Map<Symbol, DocTheory> map) {
    return new InMemoryDocTheoryCache(map);
  }
}
