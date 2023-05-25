package com.bbn.serif.util;

import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpReader;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.patterns.PatternSetFactory;
import com.bbn.serif.patterns.matching.MentionPatternMatch;
import com.bbn.serif.patterns.matching.PatternMatch;
import com.bbn.serif.patterns.matching.PatternReturns;
import com.bbn.serif.patterns.PatternSet;
import com.bbn.serif.patterns.matching.PatternSetMatcher;
import com.bbn.serif.patterns.matching.PatternSetMatcher.DocumentPatternMatcher;
import com.bbn.serif.theories.DocTheory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public final class PatternMatcherTester {

  public static void main(String argv[]) throws IOException {
    if (argv.length != 2) {
      System.err.println("Expected: pattern-file serifxml-file");
      System.exit(1);
    }


    String patternFile = argv[0];
    String serifxmlFile = argv[1];

    String patternFileContents = new String(Files.readAllBytes(Paths.get(patternFile)));


    // Create Sexp from patternString
    SexpReader sexpReader = SexpReader.builder().build();
    Sexp patternSetSexp = sexpReader.read(patternFileContents);

    // Create PatternSet and PatternSetMatcher from Sexp
    PatternSet patternSet = PatternSetFactory.fromSexp(patternSetSexp);
    PatternSetMatcher matcher = PatternSetMatcher.of(patternSet);


    // Load DocTheory
    final SerifXMLLoader loader = SerifXMLLoader.builder().build();
    DocTheory docTheory = loader.loadFrom(new File(serifxmlFile));

    // Create DocumentPatternSetMatcher from PatternSetMatcher and DocTheory
    DocumentPatternMatcher documentPatternMatcher = matcher.inContextOf(docTheory);


    // Match PatternSet against document
    PatternReturns patternReturns = documentPatternMatcher.findMatchesInDocument();

    // Read matches
    for (PatternMatch patternMatch : patternReturns.matches()) {
      System.out.println("Found pattern match!");
      System.out.println(patternMatch.spanning().get());
    }
  }

}
