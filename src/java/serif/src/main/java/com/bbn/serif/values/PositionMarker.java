package com.bbn.serif.values;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.AbstractPrivateParameterizedModule;
import com.bbn.bue.common.TextGroupEntryPoint;
import com.bbn.bue.common.TextGroupEntryPoints;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.SerifEnvironmentM;
import com.bbn.serif.driver.AbstractConstraintlessProcessingStep;
import com.bbn.serif.driver.ProcessingStep;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.patterns2.PatternMatch;
import com.bbn.serif.patterns2.PatternReturns;
import com.bbn.serif.patterns2.PatternSet;
import com.bbn.serif.patterns2.PatternSetMatcher;
import com.bbn.serif.patterns2.RegexPattern;
import com.bbn.serif.patterns2.TextPattern;
import com.bbn.serif.patterns2.TokenSpanPatternMatch;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.ValueMentions;
import com.bbn.serif.types.ValueType;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Exposed;
import com.google.inject.Provides;
import com.google.inject.multibindings.Multibinder;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;

import static com.bbn.serif.SerifEnvironmentM.preprocessorBindingKey;

/**
 * Given a file of head words of positions/titles, will add
 * {@link com.bbn.serif.theories.ValueMention}s of type {@code Job-Title} to the documents.
 */
@TextGroupImmutable
@Value.Immutable
public abstract class PositionMarker extends AbstractConstraintlessProcessingStep {

  private static final Logger log = LoggerFactory.getLogger(PositionMarker.class);

  private static final ValueType JOB_TITLE = ValueType.parseDottedPair("Job-Title");

  abstract PatternSetMatcher titleWordsMatcher();

  @Override
  public DocTheory process(final DocTheory inputDoc) {
    final PatternSetMatcher.DocumentPatternMatcher matcher =
        titleWordsMatcher().inContextOf(inputDoc);

    boolean docDirty = false;
    final DocTheory.Builder ret = inputDoc.modifiedCopyBuilder();
    for (final SentenceTheory sentenceTheory : inputDoc) {
      final PatternReturns matches = matcher.findMatchesIn(sentenceTheory);

      if (matches.matched()) {
        final ValueMentions.Builder newVMs = new ValueMentions.Builder()
            .from(sentenceTheory.valueMentions());
        for (final PatternMatch match : matches.matches()) {
          if (match.spanning().isPresent() && match instanceof TokenSpanPatternMatch) {
            final TokenSequence.Span matchSpan = match.spanning().get().span();
            log.info("In {} matches {} at offsets {}", inputDoc.docid(),
                matchSpan.tokenizedText(), matchSpan.charOffsetRange());
            newVMs.addValueMentions(ValueMention.builder(JOB_TITLE, matchSpan).build());
          }
        }

        ret.replacePrimarySentenceTheory(sentenceTheory,
            sentenceTheory.withValueMentions(newVMs.build()));
        docDirty = true;
      }
    }

    if (docDirty) {
      return ret.build();
    } else {
      return inputDoc;
    }
  }

  public static PositionMarker forTitleWords(final Iterable<String> titleWords) {
    final ImmutableList.Builder<RegexPattern> ret = ImmutableList.builder();

    for (final String titleWord : titleWords) {
      ret.add(new RegexPattern.Builder().addSubpatterns(TextPattern.of(titleWord))
          .matchWholeExtent(true).build());
    }

    return ImmutablePositionMarker.builder().titleWordsMatcher(
        PatternSetMatcher.of(PatternSet.of(ret.build()))).build();
  }


  final static class FromParamsModule extends AbstractPrivateParameterizedModule {

    protected FromParamsModule(final Parameters parameters) {
      super(parameters);
    }

    @Override
    public void configure() {
    }

    @Exposed
    @Provides
    public PositionMarker getPositionMarker(Parameters params) throws IOException {
      final File titleWordsListFile =
          params.getExistingFile("com.bbn.serif.values.titleWordsListFile");
      final ImmutableList<String> titleWordsList =
          FileUtils.loadStringList(Files.asCharSource(titleWordsListFile, Charsets.UTF_8));
      log.info("Will apply {} word title list", titleWordsList.size());
      return PositionMarker.forTitleWords(titleWordsList);
    }
  }

  final static class AsPreprocessorModule extends AbstractParameterizedModule {

    protected AsPreprocessorModule(final Parameters parameters) {
      super(parameters);
    }

    @Override
    public void configure() {
      install(new FromParamsModule(params()));
      final Multibinder<ProcessingStep> processingStepMultibinder =
          Multibinder.newSetBinder(binder(), preprocessorBindingKey());
      processingStepMultibinder.addBinding().to(PositionMarker.class);

    }
  }

  public static void main(String[] args) throws Exception {
    TextGroupEntryPoints.runEntryPoint(StandaloneEntryPoint.class, args);
  }

  private static class StandaloneEntryPoint implements TextGroupEntryPoint {

    private final SerifXMLLoader loader;
    private final SerifXMLWriter writer;
    private final PositionMarker positionMarker;
    private final Parameters params;

    @Inject
    private StandaloneEntryPoint(final PositionMarker positionMarker,
        final SerifXMLLoader loader, final SerifXMLWriter writer, final Parameters params) {
      this.positionMarker = positionMarker;
      this.loader = loader;
      this.writer = writer;
      this.params = params;
    }

    @Override
    public void run() throws Exception {
      final File inputFileMapFile = params.getExistingFile("com.bbn.serif.values.inputMap");
      final File outputFileMapFile = params.getCreatableFile("com.bbn.serif.values.outputMap");
      final File outputDirectory =
          params.getCreatableDirectory("com.bbn.serif.values.outputDirectory");

      final ImmutableMap.Builder<Symbol, File> outputMapB = ImmutableMap.builder();

      final ImmutableMap<Symbol, File> inputFiles =
          FileUtils.loadSymbolToFileMap(Files.asCharSource(inputFileMapFile, Charsets.UTF_8));
      for (final Map.Entry<Symbol, File> e : inputFiles.entrySet()) {
        final Symbol docId = e.getKey();
        final DocTheory inputDoc = loader.loadFrom(e.getValue());

        final File outputFile =
            new File(outputDirectory, inputDoc.docid().asString() + ".serifxml");
        writer.saveTo(positionMarker.process(inputDoc),
            Files.asCharSink(outputFile, Charsets.UTF_8));

        outputMapB.put(docId, outputFile);
      }

      final ImmutableMap<Symbol, File> outputMap = outputMapB.build();
      log.info("Writing output file map of {} files to {}", outputMap.size(), outputFileMapFile);
      FileUtils
          .writeSymbolToFileMap(outputMap, Files.asCharSink(outputFileMapFile, Charsets.UTF_8));
    }

    public static class FromParamsModule extends AbstractParameterizedModule {

      protected FromParamsModule(final Parameters parameters) {
        super(parameters);
      }

      @Override
      public void configure() {
        install(new SerifEnvironmentM(params()));
        install(new PositionMarker.FromParamsModule(params()));
      }
    }
  }
}
