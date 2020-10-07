package com.bbn.serif.util;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.Relations;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Removes RelationMentions and document level Relations from SerifXML files
 */
public final class StripRelations {

  private static final Logger log = LoggerFactory.getLogger(StripRelations.class);

  private StripRelations() {
    throw new UnsupportedOperationException();
  }

  public static void main(String[] argv) throws IOException {
    final Parameters params = Parameters.loadSerifStyle(new File(argv[0]));
    final File inputFileList = params.getExistingFile("inputFileList");
    final File outputDirectory = params.getAndMakeDirectory("outputDirectory");
    final File strippedFileList = params.getCreatableFile("strippedFileList");

    log.info(
        "Stripping events from files in list {}; writing results to directory {} with output manifest {}",
        inputFileList, outputDirectory, strippedFileList);

    final SerifXMLLoader loader = SerifXMLLoader.builder().build();
    final SerifXMLWriter writer = SerifXMLWriter.create();

    final List<String> outputPaths = Lists.newArrayList();

    for (final File inputFile : FileUtils.loadFileList(inputFileList)) {
      final DocTheory dt = loader.loadFrom(inputFile);
      final DocTheory.Builder newDT = dt.modifiedCopyBuilder();
      newDT.relations(Relations.absent());
      for (int sentIdx = 0; sentIdx < dt.numSentences(); ++sentIdx) {
        final SentenceTheory initialSent = dt.sentenceTheory(sentIdx);
        newDT.replacePrimarySentenceTheory(initialSent,
            stripRelations(initialSent));
      }

      String outFilename = dt.docid().toString();
      if (dt.docid().toString().lastIndexOf("/") != -1) {
        outFilename = dt.docid().toString().substring(dt.docid().toString().lastIndexOf("/") + 1);
      }
      if (!outFilename.endsWith(".xml")) {
        outFilename = outFilename.concat(".xml");
      }

      final File outputFile = new File(outputDirectory, outFilename);
      log.info("Stripping relations from {}, writing to {}", inputFile, outputFile);
      writer.saveTo(newDT.build(), outputFile);
      outputPaths.add(outputFile.getAbsolutePath());
    }
    log.info("Writing list of {} stripped files to {}", outputPaths.size(), strippedFileList);
    Files.asCharSink(strippedFileList, Charsets.UTF_8).writeLines(outputPaths);
  }

  private static SentenceTheory stripRelations(SentenceTheory st) {
    final SentenceTheory.Builder builder = st.modifiedCopyBuilder();
    builder.relationMentions(RelationMentions.absent());
    return builder.build();
  }
}
