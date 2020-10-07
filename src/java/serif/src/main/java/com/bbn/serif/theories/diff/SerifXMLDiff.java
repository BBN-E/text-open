package com.bbn.serif.theories.diff;

import com.bbn.bue.common.AbstractParameterizedModule;
import com.bbn.bue.common.TextGroupEntryPoint;
import com.bbn.bue.common.TextGroupEntryPoints;
import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.SerifEnvironmentM;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharSink;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;

public final class SerifXMLDiff implements TextGroupEntryPoint {
  private static final Logger log = LoggerFactory.getLogger(SerifXMLDiff.class);

  private final DocTheoryDiffer docTheoryDiffer;
  private final SerifXMLLoader loader;
  private final Parameters params;

  @Inject
  SerifXMLDiff(final DocTheoryDiffer docTheoryDiffer, final SerifXMLLoader loader,
      Parameters params) {
    this.docTheoryDiffer = docTheoryDiffer;
    this.loader = loader;
    this.params = params;
  }

  @Override
  public void run() throws IOException {
    final File outputDir = params.getCreatableDirectory("com.bbn.serif.diff.outputDir");
    final ImmutableMap<Symbol, File> leftDocIdToFileMap = FileUtils.loadSymbolToFileMap(
        params.getExistingFile("com.bbn.serif.diff.leftFileMap"));
    final ImmutableMap<Symbol, File> rightDocIdToFileMap = FileUtils.loadSymbolToFileMap(
        params.getExistingFile("com.bbn.serif.diff.rightFileMap"));

    outputDir.mkdirs();
    writeLeftRightOnlyFiles(outputDir, leftDocIdToFileMap, rightDocIdToFileMap);
    final List<String> docIdsWithDiffs = Lists.newArrayList();
    final List<String> docIdsWithoutDiffs = Lists.newArrayList();

    for (final Symbol docId : Sets
        .intersection(leftDocIdToFileMap.keySet(), rightDocIdToFileMap.keySet())) {
      final File fileDiffOutDir = new File(outputDir, docId.asString());
      final boolean filesDiffer = computeAndWriteFileDiff(
          loader.loadFrom(leftDocIdToFileMap.get(docId)),
          loader.loadFrom(rightDocIdToFileMap.get(docId)), fileDiffOutDir);
      if (filesDiffer) {
        log.info("{} differ", docId);
        docIdsWithDiffs.add(docId.asString());
      } else {
        log.info("{} do not differ", docId);
        docIdsWithoutDiffs.add(docId.asString());
      }
    }

    // write lists of doc IDs which do and do not match
    Files.asCharSink(new File(outputDir, "differingDocs.txt"), Charsets.UTF_8)
        .writeLines(docIdsWithDiffs);
    Files.asCharSink(new File(outputDir, "sameDocs.txt"), Charsets.UTF_8)
        .writeLines(docIdsWithDiffs);
  }

  // returns true if there is a difference
  private boolean computeAndWriteFileDiff(final DocTheory left, final DocTheory right, final File outDir)
      throws IOException {
    final Optional<DocTheoryDiff> diff = docTheoryDiffer.diff(left, right);
    if (diff.isPresent()) {
      outDir.mkdirs();
      final CharSink outFile = Files.asCharSink(new File(outDir, "diff.txt"), Charsets.UTF_8);
      final StringBuilder sb = new StringBuilder();
      diff.get().writeTextReport(sb, 0);
      outFile.write(sb.toString());
    }
    return diff.isPresent();
  }

  private void writeLeftRightOnlyFiles(File outputDir, Map<Symbol, File> leftDocIdToFileMap,
      Map<Symbol, File> rightDocIdToFileMap) throws IOException {
    final File leftOnlyFile = new File(outputDir, "leftOnly.txt");
    final File rightOnlyFile = new File(outputDir, "rightOnly.txt");

    FileUtils.writeSymbolToFileMap(Maps.filterKeys(leftDocIdToFileMap,
        not(in(rightDocIdToFileMap.keySet()))), Files.asCharSink(leftOnlyFile, Charsets.UTF_8));

    FileUtils.writeSymbolToFileMap(Maps.filterKeys(rightDocIdToFileMap,
        not(in(leftDocIdToFileMap.keySet()))), Files.asCharSink(rightOnlyFile, Charsets.UTF_8));
  }

  public static void main(String[] args) throws Exception {
    TextGroupEntryPoints.runEntryPoint(SerifXMLDiff.class, args);
  }

  static final class FromParamsModule extends AbstractParameterizedModule {
    FromParamsModule(final Parameters params) {
      super(params);
    }

    @Override
    protected void configure() {
      install(new SerifEnvironmentM(params()));
      install(new DiffersM());
    }
  }

  private static final class DiffersM extends AbstractModule {
    @Override
    protected void configure() {

    }

    @Provides
    @IgnoreScoresWhenDiffingP
    Boolean getIgnoreScores(Parameters params) {
      final boolean ignoreScores =
          params.getOptionalBoolean(params.getParamForAnnotation(IgnoreScoresWhenDiffingP.class))
              .or(false);
      if (ignoreScores) {
        log.info("Scores will be ignored");
      }
      return ignoreScores;
    }
  }
}

