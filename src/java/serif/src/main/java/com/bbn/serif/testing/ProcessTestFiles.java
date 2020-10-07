package com.bbn.serif.testing;

import com.bbn.bue.common.files.FileUtils;
import com.bbn.serif.httpclient.SerifHTTPClient;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class ProcessTestFiles {

  private static final Logger log = LoggerFactory.getLogger(ProcessTestFiles.class);

  private ProcessTestFiles() {
    throw new UnsupportedOperationException();
  }

  private static void usage() {
    System.err.println("usage: ProcessTestFiles ~/repos/jserif/serif");
  }

  private static void trueMain(String[] argv) throws IOException {
    if (argv.length != 1) {
      usage();
      System.exit(1);
    }

    final File repoBase = new File(argv[0]);
    final File resourcesDir = new File(new File(new File(repoBase, "src"),
        "test"), "resources");
    final File filesToParseFile = new File(resourcesDir, "testFilesToProcess.txt");

    if (!filesToParseFile.isFile()) {
      throw new FileNotFoundException(
          String.format("Not found: %s", filesToParseFile.getAbsolutePath()));
    }

    final SerifHTTPClient client = new SerifHTTPClient("http://localhost:8000/SerifXMLRequest");
    final SerifXMLWriter writer = SerifXMLWriter.create();
    for (final File f : FileUtils.loadFileListRelativeTo(filesToParseFile, resourcesDir)) {
      log.info("Processing {}", f);
      final DocTheory dt = client.processDocument("dummy", "English",
          Files.asCharSource(f, Charsets.UTF_8).read());
      writer.saveTo(dt, FileUtils.swapExtension(f, "xml"));
    }
  }

  public static void main(String[] argv) {
    try {
      System.err.println("Copyright 2015 Raytheon BBN Technologies Corp.");
      System.err.println("All Rights Reserved.");
      trueMain(argv);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
