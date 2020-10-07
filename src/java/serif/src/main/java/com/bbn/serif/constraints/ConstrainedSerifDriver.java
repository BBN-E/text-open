package com.bbn.serif.constraints;


import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Qualifier;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Beta
public final class ConstrainedSerifDriver {

  private static final Logger log = LoggerFactory.getLogger(ConstrainedSerifDriver.class);

  private final ConstrainedSerif jacSerif;
  private final SerifXMLWriter serifXMLWriter;
  private final ConstraintSource constraintSource;
  private final File outputDir;
  private final File docIDToSourceMap;
  private final File inputList;

  @Inject
  private ConstrainedSerifDriver(final ConstrainedSerif jacSerif, final SerifXMLWriter serifXMLWriter,
      final ConstraintSource constraintSource,
      @OutputDirP final File outputDir,
      @OriginalTextMapP final File docIDToSourceMap,
      @FileListP final File inputList) {
    this.jacSerif = jacSerif;
    this.serifXMLWriter = serifXMLWriter;
    this.constraintSource = constraintSource;
    this.outputDir = outputDir;
    this.docIDToSourceMap = docIDToSourceMap;
    this.inputList = inputList;
  }

  public void go() throws IOException {
    final ImmutableMap<Symbol, File> originalTextMap =
        FileUtils.loadSymbolToFileMap(docIDToSourceMap);
    final ImmutableList<Symbol> docIDs = FileUtils.loadSymbolList(inputList);
    for(final Symbol docID: docIDs) {
      final File doc = checkNotNull(originalTextMap.get(docID), "Could not find a document for {}" + docID);
      log.info("Processing {} from {} ", docID, doc.getAbsolutePath());
      final SerifConstraints constraints = constraintSource.constraintsForDocument(docID);
      final DocTheory dt = jacSerif.processFile(doc, docID, constraints);
      serifXMLWriter.saveTo(dt, new File(outputDir, docID.asString() + ".xml"));
    }
    jacSerif.finish();
  }

  public static final class DriverM extends AbstractModule {
    @Override
    protected void configure() {
    }

    @Provides
    @OutputDirP
    File getOutputDir(Parameters params) {
      return params.getCreatableDirectory(OutputDirP.param);
    }

    @Provides
    @OriginalTextMapP
    File getOriginalTextMap(Parameters params) {
      return params.getExistingFile(OriginalTextMapP.param);
    }

    @Provides
    @FileListP
    File getInputFileList(Parameters params) {
      return params.getExistingFile(FileListP.param);
    }
  }

  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface FileListP {
    String param = "com.bbn.serif.inputList";
  }


  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface OutputDirP {

    String param = "com.bbn.serif.outputDir";
  }


  @Qualifier
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface OriginalTextMapP {

    String param = "com.bbn.serif.originalTextMap";
  }
}

