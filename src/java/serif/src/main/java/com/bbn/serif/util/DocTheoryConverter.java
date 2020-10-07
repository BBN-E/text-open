package com.bbn.serif.util;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.TextGroupEntryPoint;
import com.bbn.bue.common.TextGroupEntryPoints;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.DocTheoryPreprocessorP;
import com.bbn.serif.SerifEnvironmentM;
import com.bbn.serif.driver.ProcessingStep;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Optional;
import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;


/**
 * Reads, converts DocTheories generated from the input files {@code inputFile} with a set of
 * Preprocessors {@code preprocessors}, writes, and re-reads SerifXML documents.
 *
 * Requires {@code @DocTheoryPreprocessorP} parameters.
 *
 * This can run in single file mode or batch mode.  In single file mode, the parameter
 * {@code inputFile} for the file to round-trip is required. You may optionally specify
 * {@code outputFile} if you want the re-written copy to persist for inspection beyond
 * the execution of this program.  In batch mode, specify {@code inputList}, a list of
 * paths to SerifXML files, and they will all be round-tripped.
 */
public final class DocTheoryConverter implements TextGroupEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(JSerifRoundTripper.class);

  private static final String INPUT_LIST_PARAM = "inputList";
  private static final String OUTPUT_DIR_PARAM = "outputDir";

  private static final String INPUT_FILE_PARAM = "inputFile";
  private static final String OUTPUT_FILE_PARAM = "outputFile";

  private final Parameters parameters;
  private final SerifXMLLoader loader;
  private final SerifXMLWriter writer;
  private final Set<ProcessingStep> preprocessors;

  @Inject
  private DocTheoryConverter(Parameters parameters, SerifXMLLoader loader, SerifXMLWriter writer,
      @DocTheoryPreprocessorP Set<ProcessingStep> preprocessors) {
    this.parameters = parameters;
    this.loader = loader;
    this.writer = writer;
    this.preprocessors = preprocessors;
  }

  public static DocTheoryConverter create(Parameters parameters, SerifXMLLoader loader, SerifXMLWriter writer,
      @DocTheoryPreprocessorP Set<ProcessingStep> preprocessors) {
    return new DocTheoryConverter(parameters, loader, writer, preprocessors);
  }

  public static void main(String... args) throws Exception {
    TextGroupEntryPoints.runEntryPoint(DocTheoryConverter.class, args);
  }

  @Override
  public void run() throws Exception {
    if (parameters.isPresent(INPUT_LIST_PARAM)) {
      checkState(!parameters.isPresent(OUTPUT_FILE_PARAM),
          "Must specify an output dir or nothing when converting a file list.");
      final List<File> inputFiles =
          FileUtils.loadFileList(parameters.getExistingFile(INPUT_LIST_PARAM));
      final Optional<File> outputDir = parameters.getOptionalCreatableDirectory(OUTPUT_DIR_PARAM);
      for (final File inputFile : inputFiles) {
        final File outputFile;
        if (outputDir.isPresent()) {
          outputFile = new File(outputDir.get(), inputFile.getName());
        } else {
          outputFile = File.createTempFile(inputFile.getName(), "tmp");
          outputFile.deleteOnExit();
        }
        this.convert(inputFile, outputFile);}
    } else {
      checkState(!parameters.isPresent(OUTPUT_DIR_PARAM),
          "Can specify an output file or nothing when converting a file.");
      final File outputFile;
      if (parameters.isPresent(OUTPUT_FILE_PARAM)) {
        outputFile = parameters.getCreatableFile(OUTPUT_FILE_PARAM);
      } else {
        outputFile = File.createTempFile(parameters.getExistingFile(INPUT_FILE_PARAM).getName(), "tmp");
        outputFile.deleteOnExit();
      }
      this.convert(parameters.getExistingFile(INPUT_FILE_PARAM), outputFile);
    }

  }


  public void convert(java.io.File input, File scratch) throws Exception {
    log.info("Converting DocTheory {}", input.getAbsolutePath());

    DocTheory in = loader.loadFrom(input);
    for (final ProcessingStep preprocessor : preprocessors) {
      in = preprocessor.process(in);
    }
    writer.saveTo(in, scratch);
    loader.loadFrom(scratch);
  }

  public final static class Module extends AbstractParameterizedModule {

    protected Module(final Parameters parameters) {
      super(parameters);
    }

    @Override
    public void configure() {
      install(new SerifEnvironmentM(params()));
    }
  }

}
