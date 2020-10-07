package com.bbn.serif.io;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Optional;

import java.io.File;

/**
 * Represents a mapping from document IDs to files.  See {@link com.bbn.nlp.io.DocIDToFileMappings}
 * for how to create instances.
 * @deprecated delete after 09/30/2016 - use {@see {@link com.bbn.nlp.io.DocIDToFileMappings}}
 */
@Deprecated
public interface DocIDToFileMapping extends com.bbn.nlp.io.DocIDToFileMapping  {

  /**
   * Returns the {@link java.io.File} corresponding to the specified document ID, if possible.
   * Otherwise, returns {@link com.google.common.base.Optional#absent()}. Beware that event if
   * absent is not returned, there is no guarantee the specified file exists.
   *
   * @param docID May not be null. If it is, the result is undefined.
   */
  @Override
  Optional<File> fileForDocID(Symbol docID);
}
