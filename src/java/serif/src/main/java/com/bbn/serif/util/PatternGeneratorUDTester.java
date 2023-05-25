package com.bbn.serif.util;

import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.patterns.PatternGeneratorUD;
import com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.ValueMention;
import com.bbn.bue.common.symbols.Symbol;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.ARGUMENT_ROLE;
import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.LIMIT_ARGS_TO_2;
import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.LOWEST_STARTING_PROP_ONLY;
import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.NO_LIST_PROPS;
import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.PREDICATE_HEAD;
import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.PROPOSITION_ARGUMENT;
import static com.bbn.serif.patterns.PatternGeneratorUD.CONSTRAINTS.PRUNE_SIMPLE_REFS;


public final class PatternGeneratorUDTester {


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
        CONSTRAINTS.LIMIT_ARGS_TO_2,
        CONSTRAINTS.LOWEST_STARTING_PROP_ONLY
    );



    Set<CONSTRAINTS> propConstraints = new HashSet<>(propConstraintsList);

    List<CONSTRAINTS> learnitEventEventConstraintList = Arrays.asList(
        PREDICATE_HEAD, PROPOSITION_ARGUMENT, ARGUMENT_ROLE,
        PRUNE_SIMPLE_REFS, NO_LIST_PROPS, LOWEST_STARTING_PROP_ONLY, LIMIT_ARGS_TO_2
    );
    Set<CONSTRAINTS> learnitEventEventConstraints = new HashSet<>(learnitEventEventConstraintList);

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
    String type = argv[1];

    final SerifXMLLoader loader = SerifXMLLoader.builder().build();
    DocTheory docTheory = loader.loadFrom(serifxmlFile);

    long startTime = System.nanoTime();

    PatternGeneratorUD patternGenerator = new PatternGeneratorUD(docTheory, mentionPremodStopWords, true, 3);
    for (SentenceTheory st : docTheory.sentenceTheories()) {

      if (type.equals("unary-event") || type.equals("all")) {
        for (EventMention em : st.eventMentions()) {

          List<Pattern> propPatterns = patternGenerator.generateUnaryPropPatterns(
              em, 2, learnitPropConstraints1, new HashSet<Symbol>(), new HashSet<Symbol>());

          printPatterns(propPatterns, "UNARY EVENT");
        }
      }

      if (type.equals("unary-mention") || type.equals("all")) {
        for (Mention m : st.mentions()) {
          List<Pattern> mentionPatterns = patternGenerator.generateUnaryMentionPatterns(
              m, mentionConstraints);

          printPatterns(mentionPatterns, "UNARY MENTION");
        }
      }

      if (type.equals("binary-event-event") || type.equals("all")) {
        for (EventMention em1 : st.eventMentions()) {
          for (EventMention em2 : st.eventMentions()) {
            if (em1 == em2)
              continue;

            List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
                em1, em2, 2, learnitEventEventConstraints, new HashSet<Symbol>(), new HashSet<Symbol>());

            printPatterns(propPatterns, "EVENT-EVENT PROP");

//            List<Pattern> lexicalPatterns = patternGenerator.generateBinaryLexicalPatterns(
//                em1, em2, 3, mentionConstraints);

//            printPatterns(lexicalPatterns, "EVENT-EVENT LEXICAL");
          }
        }
      }

      if (type.equals("binary-event-mention") || type.equals("all")) {
        for (EventMention em : st.eventMentions()) {
          for (Mention m : st.mentions()) {

            List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
                em, m, 2, propConstraints, new HashSet<Symbol>(), new HashSet<Symbol>());

            printPatterns(propPatterns, "EVENT-MENTION PROP");

            List<Pattern> lexicalPatterns = patternGenerator.generateBinaryLexicalPatterns(
                em, m, 5, mentionConstraints);

            printPatterns(lexicalPatterns, "EVENT-MENTION LEXICAL");
          }
        }
      }

      if (type.equals("binary-mention-mention") || type.equals("all")) {
        for (Mention m1 : st.mentions()) {
          for (Mention m2 : st.mentions()) {

            if (m1 == m2)
              continue;

            List<Pattern> propPatterns = patternGenerator.generateBinaryPropPatterns(
                m1, m2, 2, propConstraints, new HashSet<Symbol>(), new HashSet<Symbol>());

            printPatterns(propPatterns, "MENTION-MENTION PROP");
          }
        }
      }
    }
    long endTime = System.nanoTime();

    System.out.println("Milliseconds: " + ((endTime - startTime) / 1000000));
  }

  public static void printPatterns(List<Pattern> patterns, String label) {
    for (Pattern p : patterns) {
      System.out.println(label);
      System.out.println(p.toString() + "\n");
      System.out.println(p.toPrettyString() + "\n");
    }
  }
}
