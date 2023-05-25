package com.bbn.serif.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.patterns.PatternGenerator;
import com.bbn.serif.patterns.PatternGeneratorUD;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.patterns.Pattern;


public final class PatternGeneratorCoverageTester {

  public static void main(String argv[]) {
    List<PatternGenerator.CONSTRAINTS> propConstraintsList = Arrays.asList(
            PatternGenerator.CONSTRAINTS.PREDICATE_HEAD,
            PatternGenerator.CONSTRAINTS.MENTION_ARGUMENT,
            PatternGenerator.CONSTRAINTS.MENTION_ENTITY_TYPE,
            PatternGenerator.CONSTRAINTS.PROPOSITION_ARGUMENT,
            PatternGenerator.CONSTRAINTS.ARGUMENT_ROLE
    );
    Set<PatternGenerator.CONSTRAINTS> propConstraints = new HashSet<>(propConstraintsList);
      List<PatternGeneratorUD.CONSTRAINTS> udConstraintsList = Arrays.asList(
              PatternGeneratorUD.CONSTRAINTS.PREDICATE_HEAD,
              PatternGeneratorUD.CONSTRAINTS.MENTION_ARGUMENT,
              PatternGeneratorUD.CONSTRAINTS.MENTION_ENTITY_TYPE,
              PatternGeneratorUD.CONSTRAINTS.PROPOSITION_ARGUMENT,
              PatternGeneratorUD.CONSTRAINTS.ARGUMENT_ROLE
      );
      Set<PatternGeneratorUD.CONSTRAINTS> udConstraints = new HashSet<>(udConstraintsList);

    List<String> mentionPremodStopWordsList = Arrays.asList(
        "of", "the", "this", "his", "hers", "a", "an"
    );
    Set<String> mentionPremodStopWords = new HashSet<>(mentionPremodStopWordsList);


    File fileList = new File(argv[0]);
    BufferedReader reader;
    final SerifXMLLoader loader = SerifXMLLoader.builder().build();
    try {
      reader = new BufferedReader(new FileReader(fileList));
      String line = reader.readLine();
      while (line != null) {
        System.out.println(line);
        String content = new String(Files.readAllBytes(Paths.get(line)));
        content = content.replace(" version=\"18\"", "");
        DocTheory docTheory = loader.loadFrom(content);
        PatternGenerator patternGenerator = new PatternGenerator(docTheory, mentionPremodStopWords);
        PatternGeneratorUD patternGeneratorUD = new PatternGeneratorUD(docTheory, mentionPremodStopWords, true, 3);

        for (SentenceTheory st : docTheory.sentenceTheories()) {
         for (EventMention em: st.eventMentions()) {
          EventMention.Anchor anchor = em.anchors().get(0);
          SynNode anchorNode = anchor.anchorNode();

          System.out.println("Anchor node: " + anchorNode.toString());
          List<Pattern> patterns =
              patternGenerator.generateUnaryPropPatterns(
                  anchorNode, 2, propConstraints);
          System.out.println("Found: " + patterns.size() + " PropPatterns");
            patterns = patternGeneratorUD.generateUnaryPropPatterns(em, 2, udConstraints, new HashSet<Symbol>(), new HashSet<Symbol>());
             System.out.println("Found: " + patterns.size() + " UDPatterns");
/*          for (Pattern p : patterns) {
            System.out.println(p.toString() + "\n");
          }*/
         }
        }



          line = reader.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();

    }
  }

}
