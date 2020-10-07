package com.bbn.serif;

import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

/**
 * Transforms the raw text of a document into a {@link DocTheory}. Typically
 * the resulting {@link DocTheory} will consist only of the original text, perhaps with metadata,
 * {@link com.bbn.serif.theories.Region}s, and {@link com.bbn.serif.theories.Zoning}.
 */
@Beta
public interface TextSerifIngester extends Finishable {

  DocTheory ingestToDocTheory(Symbol docId, UnicodeFriendlyString text);
}

