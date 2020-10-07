package com.bbn.serif;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.languages.SerifLanguage;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Document;

import com.google.common.annotations.Beta;

import org.immutables.value.Value;

/**
 * Ingests plain text document to {@link DocTheory}s.
 *
 * The {@link SerifLanguage} of the input documents must be specified.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class PlainTextSerifIngester implements TextSerifIngester {

  public abstract SerifLanguage serifLanguage();

  @Override
  public DocTheory ingestToDocTheory(final Symbol docId, final UnicodeFriendlyString text) {
    final LocatedString originalText = LocatedString.fromReferenceString(text);

    final Document doc = Document.withNameAndLanguage(docId, serifLanguage())
        .originalText(originalText)
        .build();

    return DocTheory.builderForDocument(doc).build();
  }

  @Override
  public void finish() {

  }

  public static class Builder extends ImmutablePlainTextSerifIngester.Builder {

  }
}
