package com.bbn.serif.util;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public final class ExtractDocIDsFromSGM {

  private static final Logger log = LoggerFactory.getLogger(ExtractDocIDsFromSGM.class);

  private ExtractDocIDsFromSGM() {
    throw new UnsupportedOperationException();
  }

  public static void main(String[] argv) throws IOException {
    // wrapped to get return value right for runjobs
    try {
      trueMain(argv);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static final ImmutableList<Pattern> DOCNO_PATTERNS = ImmutableList.of(
      Pattern.compile(".*<DOCNO>(.*)</DOCNO>.*", CASE_INSENSITIVE),
      Pattern.compile(".*<DOC +id=\"(.*?)\".*>.*", CASE_INSENSITIVE),
      Pattern.compile(".*<DOCID>(.*)</DOCID>.*", CASE_INSENSITIVE));

  private static void trueMain(String[] argv) throws IOException {
    if (argv.length != 1) {
      usage();
    }
    final Parameters params = Parameters.loadSerifStyle(new File(argv[0]));
    log.info(params.dump());

    final Parameters mappingParams = params.copyNamespace("extractDocIDsFromSGM");
    final File inputFile = mappingParams.getExistingFile("inputFiles");
    final File outputFile = mappingParams.getCreatableFile("outputFile");

    final List<String> outputLines = Lists.newArrayList();

    final ImmutableMap.Builder<Symbol, File> docIdToFileMap = ImmutableMap.builder();
    for (final File sgmFile : FileUtils.loadFileList(inputFile)) {
      String docID = null;
      for (final String line : Files.asCharSource(sgmFile, Charsets.UTF_8).readLines()) {
        for (final Pattern pattern : DOCNO_PATTERNS) {
          final Matcher m = pattern.matcher(line);
          if (m.matches()) {
            docID = m.group(1).trim();
            break;
          }
        }
        if (docID != null) {
          break;
        }
      }
      if (docID == null) {
        // if we can't read a docid from the file, fall back to using the filename
        // with whitespace removed
        docID = CharMatcher.whitespace().replaceFrom(sgmFile.getName(), "_");
      }
      docIdToFileMap.put(Symbol.from(docID), sgmFile);
    }

    FileUtils
        .writeSymbolToFileMap(docIdToFileMap.build(), Files.asCharSink(outputFile, Charsets.UTF_8));
  }

  private static void usage() {
    System.err.println("Expected one argument, a parameter file, with parameters:\n"
        + "\tinputFiles: a file listing input files\n\toutputFile");
    System.exit(1);
  }
}
