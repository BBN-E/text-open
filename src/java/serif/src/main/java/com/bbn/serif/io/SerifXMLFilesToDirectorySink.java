package com.bbn.serif.io;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Qualifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


/**
 * Writes DocTheories to an output directory as SerifXML.
 */
final class SerifXMLFilesToDirectorySink implements DocTheorySink {

  private static final Logger log = LoggerFactory.getLogger(SerifXMLFilesToDirectorySink.class);

  private final SerifXMLWriter writer;
  private final File outputDirectory;
  private final CharSink processedDocListSink;
  private final CharSink processedDocMapSink;

  private final List<File> outputFiles = Lists.newArrayList();
  private final ImmutableMap.Builder<Symbol, File> outputFileMap = ImmutableMap.builder();

  @Inject
  SerifXMLFilesToDirectorySink(final SerifXMLWriter writer,
      @OutputDirectoryP final File outputDirectory,
      @ProcessedDocListP final CharSink processedDocListSink,
      @ProcessedDocMapP final CharSink processedDocMapSink) {
    this.writer = checkNotNull(writer);
    this.outputDirectory = checkNotNull(outputDirectory);
    this.processedDocListSink = checkNotNull(processedDocListSink);
    this.processedDocMapSink = checkNotNull(processedDocMapSink);
  }

  @Override
  public void finish() throws IOException {
    FileUtils.writeFileList(outputFiles, processedDocListSink);
    FileUtils.writeSymbolToFileMap(outputFileMap.build(), processedDocMapSink);
    log.info(outputFiles.size() + " files written to " + outputDirectory.toString());
  }

  @Override
  public void consume(final DocTheory docTheory) throws IOException {
    final String outFilename = docTheory.docid().asString() + ".xml";
    final File outputFile = new File(outputDirectory, outFilename);
    writer.saveTo(docTheory, outputFile);
    outputFiles.add(outputFile);
    outputFileMap.put(docTheory.docid(), outputFile);
  }

  /**
   * Path to write list of documents processed to.
   */
  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  @interface ProcessedDocListP {

    String param = "com.bbn.serif.io.processedDocumentList";
  }

  /**
   * Path to write map of docids to files for processed documents.
   */
  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  @interface ProcessedDocMapP {

    String param = "com.bbn.serif.io.processedDocumentMap";
  }
}
