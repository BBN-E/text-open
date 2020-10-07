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

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.patterns.PatternGenerator;
import com.bbn.serif.patterns.PatternGenerator.CONSTRAINTS;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.patterns.Pattern;


public final class PatternGeneratorCoverageTester {

  public static void main(String argv[]) {
    List<CONSTRAINTS> constraintsList = Arrays.asList(
        CONSTRAINTS.PREDICATE_HEAD,
        CONSTRAINTS.MENTION_ARGUMENT,
        CONSTRAINTS.MENTION_ENTITY_TYPE,
        CONSTRAINTS.PROPOSITION_ARGUMENT,
        CONSTRAINTS.ARGUMENT_ROLE
    );
    Set<CONSTRAINTS> constraints = new HashSet<>(constraintsList);

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

        for (SentenceTheory st : docTheory.sentenceTheories()) {
         for (EventMention em: st.eventMentions()) {
          EventMention.Anchor anchor = em.anchors().get(0);
          SynNode anchorNode = anchor.anchorNode();

          System.out.println("Anchor node: " + anchorNode.toString());
          List<Pattern> patterns =
              patternGenerator.generateUnaryPropPatterns(
                  anchorNode, 2, constraints);
          System.out.println("Found: " + patterns.size() + " patterns");

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
