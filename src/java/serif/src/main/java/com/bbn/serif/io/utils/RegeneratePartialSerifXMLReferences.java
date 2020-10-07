package com.bbn.serif.io.utils;

import com.bbn.serif.httpclient.SerifHTTPClient;
import com.bbn.serif.io.SerifXMLWriter;
import com.bbn.serif.theories.DocTheory;

import java.io.File;
import java.io.IOException;

public final class RegeneratePartialSerifXMLReferences {

  private RegeneratePartialSerifXMLReferences() {
    throw new UnsupportedOperationException();
  }

  private static String[] stages = new String[]{"sent-break",
      "tokens",
      "part-of-speech",
      "names",
      "values",
      "parse",
      "mentions",
      "props",
      "metonymy",
      "entities",
      "events",
      "relations",
      "prop-status",
      "doc-entities",
      "doc-relations-events",
      "doc-values",
      "confidences",
      "generics",
      "doc-actors",
      "factfinder",
      "clutter",
      "xdoc"};

  public static void main(String[] args) throws IOException {
    //Generate files for partial xml test
    // Requires a Serif English server  running on an accessible host machine
    //   Server should have par files set to expect input_type 'auto' to handle bare text
    System.err.println("Copyright 2016 Raytheon BBN Technologies.");
    System.err.println("All Rights Reserved.");
    if (args.length != 2) {
      System.err.println(
          "Expected exactly two arguments.\n"
          + "1. the URL for a SerifHTTPServer, e.g. http://localhost:8000/SerifXMLRequest\n"
          + "2. the output path, e.g. /nfs/raid66/u12/users/rbock/git/jserif/serif/src/test/resources/partialXMLtests");
    }
    final String serifHttpURL = args[0];
    final File outputDir = new File(args[1]);
    SerifHTTPClient client = new SerifHTTPClient(serifHttpURL);
    for (int i = 0; i < stages.length; i++) {
      DocTheory docTheory = client.processDocument("Stage" + i, "English",
          "The boy and the fox ran swiftly through the rain.\n" +
          "In Nicaragua today Nicaraguan troops found themselves face to face with Nicaraguan troops.\n",
          stages[i]);
      SerifXMLWriter writer = SerifXMLWriter.create();
      writer.saveTo(docTheory, new File(outputDir, "Stage" + i + ".xml"));
    }

  }
}
