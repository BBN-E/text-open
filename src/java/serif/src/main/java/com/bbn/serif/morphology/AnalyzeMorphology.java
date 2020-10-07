package com.bbn.serif.morphology;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.Finishables;
import com.bbn.bue.common.TextGroupEntryPoint;
import com.bbn.bue.common.TextGroupEntryPoints;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.SerifEnvironmentM;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import javax.inject.Inject;

/**
 * Applies a morphological analyzer to Serif documents. See {@link MorphologicalAnalyzer} for
 * how to specify the analyzer.  See {@link #run()} for parameters to specify the files to run on.
 */
public final class AnalyzeMorphology implements TextGroupEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(AnalyzeMorphology.class);

  private final Parameters params;
  private final SerifXMLLoader loader;
  private final SerifXMLWriter writer;
  private final MorphologicalAnalyzer analyzer;

  @Inject
  private AnalyzeMorphology(final Parameters params, final SerifXMLLoader loader,
      final SerifXMLWriter writer, final MorphologicalAnalyzer analyzer) {
    this.params = params;
    this.loader = loader;
    this.writer = writer;
    this.analyzer = analyzer;
  }

  @Override
  public void run() throws Exception {
    final File inputFileMapFile = params.getExistingFile("com.bbn.serif.morphology.inputFileMap");
    final ImmutableMap<Symbol, File> inputFileMap = FileUtils.loadSymbolToFileMap(inputFileMapFile);
    final File outputDirectory =
        params.getCreatableDirectory("com.bbn.serif.morphology.outputDirectory");
    final File outputMapFile = params.getCreatableFile("com.bbn.serif.morphology.outputFileMap");

    log.info("Loaded map of {} files to process from {}", inputFileMap.size(), inputFileMapFile);

    final ImmutableMap.Builder<Symbol, File> outputMapB = ImmutableMap.builder();

    for (final Map.Entry<Symbol, File> e : inputFileMap.entrySet()) {
      log.info("Processing {}", e.getKey());
      final File inputFile = e.getValue();
      // write to a file in the output directory with the same name as the input file
      // this could potentially result in a collision if input files come from different directories
      // although in typical usage where files are named by document IDs this should not happen.
      // We check for this below
      final File outputFile = new File(outputDirectory, inputFile.getName());
      outputMapB.put(e.getKey(), outputFile);
      writer.saveTo(analyzer.analyze(loader.loadFrom(inputFile)), outputFile);
    }

    final ImmutableMap<Symbol, File> outputMap = outputMapB.build();
    log.info("Wrote {} files to {} with output map", outputMap.size(), outputDirectory,
        outputMapFile);
    FileUtils.writeSymbolToFileMap(outputMap, Files.asCharSink(outputMapFile, Charsets.UTF_8));

    // check for collisions
    if (ImmutableSet.copyOf(outputMap.values()).size() != outputMap.keySet().size()) {
      throw new SerifException("One or more output files collided, see source for details");
    }

    // let the morphological analyzer write out any stats it would like to
    Finishables.finishIfApplicable(analyzer);
  }

  public static void main(String[] args) throws Exception {
    TextGroupEntryPoints.runEntryPoint(AnalyzeMorphology.class, args);
  }

  public static class FromParamsModule extends AbstractParameterizedModule {

    protected FromParamsModule(final Parameters parameters) {
      super(parameters);
    }

    @Override
    public void configure() {
      install(new SerifEnvironmentM(params()));
      install(new MorphologicalAnalyzer.FromParamsModule(params()));
    }
  }
}
