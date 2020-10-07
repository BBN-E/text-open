package com.bbn.serif.driver;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.io.DocTheoryLoader;
import com.bbn.serif.io.DocTheorySink;
import com.bbn.serif.io.SerifIOUtils;
import com.bbn.serif.theories.DocTheory;

import com.google.common.io.CharSource;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

/**
 * Runs Serif process, as configured by Guice.
 */
public final class JSerifProcessor {

  private final Parameters params;

  private final DocTheoryLoader loader;
  private final List<ProcessingStep> steps;
  private final Set<DocTheorySink> sinks;
  private final Iterable<CharSource> inputs;

  @Inject
  private JSerifProcessor(final Parameters params, final DocTheoryLoader loader,
      final List<ProcessingStep> steps, final Set<DocTheorySink> sinks,
      final Iterable<CharSource> inputs) {
    this.params = params;
    this.loader = loader;
    this.steps = steps;
    this.sinks = sinks;
    this.inputs = inputs;
  }

  private void go() throws IOException {
    for (DocTheory dt : SerifIOUtils.docTheoriesFromCharSources(inputs, loader)) {
      for (final ProcessingStep step : steps) {
        dt = step.process(dt);
      }
      for (final DocTheorySink sink : sinks) {
        sink.consume(dt);
      }
    }
    for (final ProcessingStep step : steps) {
      step.finish();
    }
    for (final DocTheorySink sink : sinks) {
      sink.finish();
    }
  }

  public static void main(String[] argv) {
    // we wrap the main method in this way to
    // ensure a non-zero return value on failure
    try {
      trueMain(argv);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void trueMain(String[] argv) throws IOException {
    throw new UnsupportedOperationException("Code is incomplete, with problematic dependencies. Disabled.");
    /*final Parameters params = Parameters.loadSerifStyle(new File(argv[0]));

    Guice.createInjector(ParametersModule.createAndDump(params),
        new SerifEnvironmentM(params),
        new JSerifProcessorFromParamsM(),
        new CSerifWithMetroParserM())
        .getInstance(JSerifProcessor.class).go();*/
  }
}
