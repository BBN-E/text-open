package com.bbn.serif.util;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.patterns.PatternGenerator;
import com.bbn.serif.patterns.PatternGenerator.CONSTRAINTS;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMention;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public final class PatternGeneratorTester {


  public static void main(String argv[]) throws IOException {
    List<CONSTRAINTS> propConstraintsList = Arrays.asList(
        CONSTRAINTS.PREDICATE_HEAD,
        CONSTRAINTS.SLOT_PREDICATE_HEAD,
        CONSTRAINTS.MENTION_HEAD_WORD,
        CONSTRAINTS.SLOT_MENTION_ENTITY_TYPE,
        CONSTRAINTS.MENTION_ENTITY_TYPE,
        CONSTRAINTS.MENTION_ARGUMENT,
        CONSTRAINTS.PROPOSITION_ARGUMENT,
        CONSTRAINTS.ARGUMENT_ROLE,
        CONSTRAINTS.VALUE_MENTION_ARGUMENT,
        CONSTRAINTS.VALUE_MENTION_VALUE_TYPE,
        CONSTRAINTS.LIMIT_ARGS_TO_2
    );
    Set<CONSTRAINTS> propConstraints = new HashSet<>(propConstraintsList);
    Set<CONSTRAINTS> mentionConstraints = new HashSet<>(
        Arrays.asList(CONSTRAINTS.SLOT_MENTION_HEAD_WORD, CONSTRAINTS.MENTION_PREMOD));

    List<CONSTRAINTS> learnitPropConstraints1List = Arrays.asList(
        CONSTRAINTS.PREDICATE_HEAD,
        CONSTRAINTS.SLOT_PREDICATE_HEAD,
        CONSTRAINTS.ARGUMENT_ROLE,
        CONSTRAINTS.PROPOSITION_ARGUMENT,
        CONSTRAINTS.MENTION_ARGUMENT,
        CONSTRAINTS.MENTION_ENTITY_TYPE);

    Set<CONSTRAINTS> learnitPropConstraints1 = new HashSet<>(learnitPropConstraints1List);

    List<String> mentionPremodStopWordsList = Arrays.asList(
        "of", "the", "this", "his", "hers", "a", "an", "or"
    );
    Set<String> mentionPremodStopWords = new HashSet<>(mentionPremodStopWordsList);

    File serifxmlFile = new File(argv[0]);

    final SerifXMLLoader loader = SerifXMLLoader.builder().build();
    DocTheory docTheory = loader.loadFrom(serifxmlFile);

    long startTime = System.nanoTime();

    PatternGenerator patternGenerator = new PatternGenerator(docTheory, mentionPremodStopWords);
    for (SentenceTheory st : docTheory.sentenceTheories()) {

      for (EventMention em : st.eventMentions()) {

        List<Pattern> propPatterns = patternGenerator.generateUnaryPropPatterns(
            em, 2, learnitPropConstraints1);

        printPatterns(propPatterns);

        List <Pattern> mentionPatterns = patternGenerator.generateUnaryMentionPatterns(
            em, mentionConstraints);

        //printPatterns(mentionPatterns);
      }

      for (EventMention em1 : st.eventMentions()) {
        for (EventMention em2 : st.eventMentions()) {
          if (em1 == em2)
            continue;

          List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
              em1, em2, 2, propConstraints);

          //printPatterns(propPatterns);


          List<Pattern> lexicalPatterns = patternGenerator.generateBinaryLexicalPatterns(
             em1, em2, 5, mentionConstraints);

          //printPatterns(lexicalPatterns);
        }
      }

      for (EventMention em : st.eventMentions()) {
        for (Mention m : st.mentions()) {

          List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
              em, m, 2, propConstraints);

          //printPatterns(propPatterns);

          List<Pattern> lexicalPatterns = patternGenerator.generateBinaryLexicalPatterns(
              em, m, 5, mentionConstraints);

          //printPatterns(lexicalPatterns);
          }
      }

      for (Proposition prop : st.propositions()) {
        if (!prop.predHead().isPresent())
          continue;

        List<Pattern> propPatterns = patternGenerator.generateUnaryPropPatterns(
            prop.predHead().get(), 2, propConstraints);

        //printPatterns(propPatterns);
      }

      for (Proposition prop : st.propositions()) {
        for (ValueMention vm : st.valueMentions()) {
          if (!prop.predHead().isPresent())
            continue;

          List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
              prop.predHead().get(), vm, 2, propConstraints);

          //printPatterns(propPatterns);
        }
      }

      for (Proposition prop1 : st.propositions()) {
        for (Proposition prop2 : st.propositions()) {

          if (prop1 == prop2)
            continue;
          if (!prop1.predHead().isPresent())
            continue;
          if (!prop2.predHead().isPresent())
            continue;

          List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
              prop1.predHead().get(), prop2.predHead().get(), 2, propConstraints);

          //printPatterns(propPatterns);

        }
      }

      for (Mention m1 : st.mentions()) {
        for (Mention m2 : st.mentions()) {

          if (m1 == m2)
            continue;

          List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
              m1, m2, 2, propConstraints);

          //printPatterns(propPatterns);
        }
      }

      for (Proposition prop : st.propositions()) {
        for (Mention m : st.mentions()) {

          if (!prop.predHead().isPresent()) continue;

          List<Pattern> propPatterns1 = patternGenerator.generateBinaryPropPatterns(
              prop.predHead().get(), m, 2, propConstraints);

          //printPatterns(propPatterns1);

          List<Pattern> propPatterns2 = patternGenerator.generateBinaryPropPatterns(
              m, prop.predHead().get(), 2, propConstraints);

          //printPatterns(propPatterns2);
        }
      }
    }
    long endTime = System.nanoTime();

    System.out.println("Milliseconds: " + ((endTime - startTime) / 1000000));
  }

  public static void printPatterns(List<Pattern> patterns) {
    for (Pattern p : patterns) {
      System.out.println(p.toString() + "\n");
      System.out.println(p.toPrettyString() + "\n");
    }
  }
}
