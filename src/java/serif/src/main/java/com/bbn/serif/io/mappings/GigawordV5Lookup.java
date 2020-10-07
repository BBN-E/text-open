package com.bbn.serif.io.mappings;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.io.DocIDToFileMapping;
import com.bbn.nlp.io.DocIDToFileMappings;

import com.google.common.base.Function;
import com.google.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Resolves doc IDs to their locations in Serif-processed Gigaword V5. <p/> The current canonical
 * location of this corpus (BBN-internally) is /nfs/mercury-04/u10/GigawordV5/serif-english/
 */
public final class GigawordV5Lookup implements Function<Symbol, Optional<File>> {

  private static final Logger log = LoggerFactory.getLogger(GigawordV5Lookup.class);

  private final File base;

  GigawordV5Lookup(File base) {
    checkArgument(base.isDirectory());
    this.base = checkNotNull(base);
  }

  public static GigawordV5Lookup createFor(File gigawordBaseDirectory) {
    return new GigawordV5Lookup(gigawordBaseDirectory);
  }

  /**
   * Creates a {@code GigawordV5Lookup} for the base directory specified by the parameter {@code
   * gigaword5Dir}.
   */
  public static DocIDToFileMapping fromParameters(Parameters params) {
    return DocIDToFileMappings
        .forFunction(new GigawordV5Lookup(params.getExistingDirectory("gigaword5Dir")));
  }


  @Override
  public Optional<File> apply(Symbol input) {
    final String docID = input.toString();
    final String prefix = docID.substring(0, 14);
    final String source = prefix.substring(0, 3);
    final File ret = new File(base, source.toLowerCase() + "/" + prefix.toLowerCase()
        + "/output/" + docID + ".xml");
    log.info("Mapped {} to {}", input, ret);
    return Optional.of(ret);
  }
}
