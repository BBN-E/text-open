package com.bbn.serif.util;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Converts a list of SerifXML files to a document-ID to file map by extracting the document IDs
 * from the SerifXML
 */
public final class GetDocidFilePathMappings {

  private GetDocidFilePathMappings() {
    throw new UnsupportedOperationException();
  }

  private static final Logger log = LoggerFactory.getLogger(GetDocidFilePathMappings.class);

  private static void usage() {
    log.info(
        "Converts a list of SerifXML files to a document-ID to file map by extracting the document IDs\n"
            + " * from the SerifXML\n"
            + "usage: GetDocidFilePathMappings [params]\n" +
            "\tdocidToFileMapper.inputFiles: <list of SerifXML files>\n"
            + "\tdocidToFileMapper.outputFile: <file to output docID to file map to\n"
            + "OR GetDocidFilePathMappings <inputFile> <outputFile>");
    System.exit(1);
  }

  public static void main(String[] argv) throws IOException {
    // wrapped to get return value right for runjobs
    try {
      trueMain(argv);
    } catch (Exception e) {
      e.printStackTrace();
      ;
      System.exit(1);
    }
  }

  private static void trueMain(String[] argv) throws IOException {
    final File inputFile;
    final File outputFile;

    if (argv.length == 1) {
      final Parameters params = Parameters.loadSerifStyle(new File(argv[0]));
      final Parameters mappingParams = params.copyNamespace("docidToFileMapper");
      inputFile = mappingParams.getExistingFile("inputFiles");
      outputFile = mappingParams.getCreatableFile("outputFile");
    } else if (argv.length == 2) {
      inputFile = new File(argv[0]);
      outputFile = new File(argv[1]);
    } else {
      usage();
      throw new RuntimeException("Can't get here");
    }

    final SerifXMLLoader loader =
        SerifXMLLoader.builder().allowSloppyOffsets().build();
    final OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(outputFile), "UTF-8");
    final List<File> serifXmlFiles = FileUtils.loadFileList(inputFile);

    for (File serifXmlFile : serifXmlFiles) {
      final DocTheory dt = loader.loadFrom(serifXmlFile);
      writer.write(dt.docid().toString() + "\t" + serifXmlFile.getPath() + "\n");
    }

    writer.close();
  }
}






