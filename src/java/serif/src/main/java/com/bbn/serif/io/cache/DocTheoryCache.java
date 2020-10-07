package com.bbn.serif.io.cache;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;

import java.io.IOException;

/**
 * A mapping from document IDs to document theories, which may be lazily loaded from disk and/or
 * cached.
 *
 * @author rgabbard
 */
public interface DocTheoryCache {

  /**
   * Gets a document theory for the specified document ID. Throws an {@link java.io.IOException} if
   * there is no theory available for the specified document ID.
   */
  public DocTheory getDocTheory(final Symbol docid) throws IOException;
}
