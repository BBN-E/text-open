package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.Proposition.PredicateType;
import com.bbn.serif.theories.Proposition.Argument;
import com.bbn.serif.theories.Proposition.MentionArgument;
import com.bbn.serif.theories.Proposition.PropositionArgument;

import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSpan;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.types.EntityType;
import com.bbn.serif.types.ValueType;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PatternGenerator {

  public enum CONSTRAINTS {
    PREDICATE_HEAD,
    SLOT_PREDICATE_HEAD,
    MENTION_ARGUMENT,
    MENTION_HEAD_WORD,
    SLOT_MENTION_HEAD_WORD,
    MENTION_ENTITY_TYPE,
    SLOT_MENTION_ENTITY_TYPE,
    VALUE_MENTION_ARGUMENT,
    VALUE_MENTION_VALUE_TYPE,
    PROPOSITION_ARGUMENT,
    ARGUMENT_ROLE,
    MENTION_PREMOD,
    LIMIT_ARGS_TO_1,
    LIMIT_ARGS_TO_2,
    PRUNE_SIMPLE_REFS,
    NO_LIST_PROPS
  }

  private final DocTheory docTheory;
  private final Set<String> mentionPremodStopWords;

  private Map<Object, Symbol> objectsToLabel = new HashMap<>();

  // These get reset whenever generatePropPatterns is called.
  private int maximumDepth;
  private Set<CONSTRAINTS> globalConstraints;

  // Map of Propositions to the Objects (Mentions and Propositions) that can be reached
  // by traversing through Propositions. This will help limit the search space
  // when looking for starting nodes to search from
  private Map<Proposition, Set<Object>> reachableObjects;

  // Map from Objects (Mentions and Propositions) to EventMentions that overlap
  private Map<Object, Set<EventMention>> objectToEventMentionCache;
  // Map from EventMention to Objects (Mentions and Propositions) that overlap
  private Map<EventMention, Set<Object>> eventMentionToObjectCache;

  // Labels
  private final Symbol SLOT_0 = Symbol.from("slot0");
  private final Symbol SLOT_1 = Symbol.from("slot1");

  public PatternGenerator(DocTheory dt, Set<String> mpsw) {
    docTheory = dt;
    mentionPremodStopWords = mpsw;

    objectToEventMentionCache = new HashMap<>();
    eventMentionToObjectCache = new HashMap<>();
    cacheMentionsToEventMentions(dt);
    cachePropositionsToEventMentions(dt);

    reachableObjects = new HashMap<>();
    cacheReachableObjects(dt);
  }

  // Align mentions to EventMentions
  private void cacheMentionsToEventMentions(DocTheory dt) {
    for (SentenceTheory st : dt.sentenceTheories()) {
      if (st.mentions().size() == 0 || st.eventMentions().size() == 0)
        continue;
      Map<SynNode, Set<Mention>> mentionCache = new HashMap<>();

      for (Mention m : st.mentions()) {
        if (m.mentionType() == Mention.Type.NONE)
          continue;
        SynNode mentionHead = m.node().headPreterminal();
        if (!mentionCache.containsKey(mentionHead))
          mentionCache.put(mentionHead, new HashSet<Mention>());
        mentionCache.get(mentionHead).add(m);
      }

      for (EventMention em : st.eventMentions()) {
        SynNode eventHead = em.anchorNode().headPreterminal();
        if (mentionCache.containsKey(eventHead)) {
          for (Mention m : mentionCache.get(eventHead)) {
            if (!objectToEventMentionCache.containsKey(m))
              objectToEventMentionCache.put(m, new HashSet<EventMention>());
            objectToEventMentionCache.get(m).add(em);
            if (!eventMentionToObjectCache.containsKey(em))
              eventMentionToObjectCache.put(em, new HashSet<>());
            eventMentionToObjectCache.get(em).add(m);

            //System.out.println("Mapping: " + m.toString() + " to: " + em.anchorNode().toString() + "\n");
          }
        }
      }
    }
  }

  // Align propositions to EventMentions
  private void cachePropositionsToEventMentions(DocTheory dt) {
    for (SentenceTheory st : dt.sentenceTheories()) {
      if (st.propositions().size() == 0 || st.eventMentions().size() == 0)
        continue;
      Map<SynNode, Set<Proposition>> propositionCache = new HashMap<>();

      for (Proposition p : st.propositions()) {
        if (!p.predHead().isPresent()) continue;
        SynNode propHead = p.predHead().get().headPreterminal();
        if (!propositionCache.containsKey(propHead))
          propositionCache.put(propHead, new HashSet<Proposition>());
        propositionCache.get(propHead).add(p);
      }

      for (EventMention em : st.eventMentions()) {
        SynNode eventHead = em.anchorNode().headPreterminal();
        if (propositionCache.containsKey(eventHead)) {
          for (Proposition p : propositionCache.get(eventHead)) {
            if (!objectToEventMentionCache.containsKey(p))
              objectToEventMentionCache.put(p, new HashSet<EventMention>());
            objectToEventMentionCache.get(p).add(em);
            if (!eventMentionToObjectCache.containsKey(em))
              eventMentionToObjectCache.put(em, new HashSet<>());
            eventMentionToObjectCache.get(em).add(p);

            //System.out.println("Mapping: " + p.toString() + " to: " + em.anchorNode().toString() + "\n");
          }
        }
      }
    }
  }

  private void cacheReachableObjects(DocTheory dt) {
    for (SentenceTheory st : dt.sentenceTheories()) {
      cacheReachableObjects(st);
    }
  }

  private void cacheReachableObjects(SentenceTheory st) {
    for (Proposition startingProp : st.propositions()) {
      reachableObjects.put(startingProp, new HashSet<>());
      traverse(startingProp, startingProp);
    }
  }

  private void traverse(Proposition startingProp, Proposition reachableProp) {

      reachableObjects.get(startingProp).add(reachableProp);
      if (objectToEventMentionCache.containsKey(reachableProp))
        reachableObjects.get(startingProp).addAll(objectToEventMentionCache.get(reachableProp));

    for (Argument a : reachableProp.args()) {
      if (a instanceof PropositionArgument) {
        traverse(startingProp, ((PropositionArgument) a).proposition());
      }
      if (a instanceof MentionArgument) {
        Mention mention = ((MentionArgument) a).mention();
        reachableObjects.get(startingProp).add(mention);
        if (objectToEventMentionCache.containsKey(mention))
          reachableObjects.get(startingProp).addAll(objectToEventMentionCache.get(mention));

        List<ValueMention> valueMentions =
            getOverlappingValueMentions(((MentionArgument)a).mention());
        for (ValueMention valueMention : valueMentions) {
          reachableObjects.get(startingProp).add(valueMention);
        }
      }
    }
  }

  public List<Pattern> generateUnaryMentionPatterns(
      SynNode synNode, Set<CONSTRAINTS> constraints) {
    globalConstraints = constraints;
    List<Pattern> results = new ArrayList<>();

    SentenceTheory st = synNode.sentenceTheory(docTheory);
    List<Mention> mentions = getAllMentionsHeadedOnSynNode(synNode, st);
    for (Mention mention : mentions) {
      objectsToLabel.clear();
      objectsToLabel.put(mention, SLOT_0);
      results.addAll(getMentionPatterns(mention));
    }

    return results;
  }

  public List<Pattern> generateUnaryMentionPatterns(
      EventMention eventMention, Set<CONSTRAINTS> constraints) {
    globalConstraints = constraints;
    List<Pattern> results = new ArrayList<>();

    if (!eventMentionToObjectCache.containsKey(eventMention))
      return new ArrayList<>();

    // Objects are either Propositions or Mentions
    for (Object o : eventMentionToObjectCache.get(eventMention)) {

      if (o instanceof Mention) {
        objectsToLabel.clear();
        objectsToLabel.put(o, SLOT_0);
        results.addAll(getMentionPatterns((Mention) o));
      }
    }

    return results;
  }

  public List<Pattern> generateUnaryPropPatterns(
      SynNode triggerSynNode, int depthConstraint, Set<CONSTRAINTS> constraints) {

    List<Pattern> results = new ArrayList<>();

    SentenceTheory st = triggerSynNode.sentenceTheory(docTheory);
    List<Proposition> triggerPropositions = getAllPropositionsFromSynNode(triggerSynNode, st);
    for (Proposition startingProposition : st.propositions()) {
      for (Proposition triggerProposition : triggerPropositions) {
        results.addAll(generateUnaryPropPatternsGeneral(
            triggerProposition, startingProposition, depthConstraint, constraints));
      }
    }
    return results;
  }

  public List<Pattern> generateUnaryPropPatterns(
      EventMention triggerEventMention, int depthConstraint, Set<CONSTRAINTS> constraints) {

    List<Pattern> results = new ArrayList<>();

    if (!eventMentionToObjectCache.containsKey(triggerEventMention))
      return new ArrayList<>();
    SentenceTheory st = triggerEventMention.sentenceTheory(docTheory);

    for (Proposition startingProposition : st.propositions()) {
      // Objects are either Propositions or Mentions
      for (Object o : eventMentionToObjectCache.get(triggerEventMention)) {
        results.addAll(generateUnaryPropPatternsGeneral(
            o, startingProposition, depthConstraint, constraints));
      }
    }
    return results;
  }

  public List<Pattern> generateUnaryPropPatternsGeneral(
      Object slot0, Proposition startingProposition,
      int depthConstraint, Set<CONSTRAINTS> constraints) {

    globalConstraints = constraints;
    List<Pattern> results = new ArrayList<>();

    if (!isValidStartingProp(startingProposition, slot0))
      return new ArrayList<>();

    objectsToLabel.clear();
    objectsToLabel.put(slot0, SLOT_0);

    List<Pattern> intermediateResults =
        new ArrayList<>(generatePropPatterns(startingProposition, depthConstraint));

    for (Pattern pattern : intermediateResults) {
      Set<Symbol> labels = getLabels((PropPattern) pattern);
      if (foundAllLabels(objectsToLabel.values(), labels))
        results.add(pattern);
    }
    return results;
  }


  // SynNode, Mention
  public List<Pattern> generateBinaryPropPatterns(
      SynNode slot0SynNode, Object slot1Mention, int depthConstraint, Set<CONSTRAINTS> constraints) {

    if (!(slot1Mention instanceof Mention || slot1Mention instanceof ValueMention))
      return new ArrayList<>();

    List<Pattern> results = new ArrayList<>();
    SentenceTheory st = slot0SynNode.sentenceTheory(docTheory);
    List<Proposition> slot0Propositions = getAllPropositionsFromSynNode(slot0SynNode, st);

    for (Proposition startingProposition : st.propositions()) {
      for (Proposition slot0Proposition : slot0Propositions) {
        results.addAll(generateBinaryPropPatternsGeneral(
            slot0Proposition, slot1Mention, startingProposition, depthConstraint, constraints));
      }
    }
    return results;
  }


  // Mention, SynNode
  public List<Pattern> generateBinaryPropPatterns(
      Object slot0Mention, SynNode slot1SynNode, int depthConstraint, Set<CONSTRAINTS> constraints) {

    if (!(slot0Mention instanceof Mention || slot0Mention instanceof ValueMention))
      return new ArrayList<>();

    List<Pattern> results = new ArrayList<>();
    SentenceTheory st = slot1SynNode.sentenceTheory(docTheory);
    List<Proposition> slot1Propositions = getAllPropositionsFromSynNode(slot1SynNode, st);
    for (Proposition startingProposition : st.propositions()) {
      for (Proposition slot1Proposition : slot1Propositions) {
        results.addAll(generateBinaryPropPatternsGeneral(
            slot0Mention, slot1Proposition, startingProposition, depthConstraint, constraints));
      }
    }
    return results;
  }

  // SynNode, SynNode
  public List<Pattern> generateBinaryPropPatterns(
      SynNode slot0SynNode, SynNode slot1SynNode, int depthConstraint,
      Set<CONSTRAINTS> constraints)
  {

    SentenceTheory st1 = slot0SynNode.sentenceTheory(docTheory);
    SentenceTheory st2 = slot1SynNode.sentenceTheory(docTheory);
    List<Proposition> propositions0 = getAllPropositionsFromSynNode(slot0SynNode, st1);
    List<Proposition> propositions1 = getAllPropositionsFromSynNode(slot1SynNode, st2);

    List<Pattern> results = new ArrayList<>();
    SentenceTheory st = slot0SynNode.sentenceTheory(docTheory);
    for (Proposition startingProposition : st.propositions()) {
      for (Proposition proposition0 : propositions0) {
        for (Proposition proposition1 : propositions1) {
          if (proposition0 == proposition1)
            continue;

          results.addAll(generateBinaryPropPatternsGeneral(
              proposition0, proposition1, startingProposition, depthConstraint, constraints));
        }
      }
    }
    return results;
  }

  // Mention, Mention
  public List<Pattern> generateBinaryPropPatterns(
      Object slot0Mention, Object slot1Mention,
      int depthConstraint, Set<CONSTRAINTS> constraints) {

    if (!(slot0Mention instanceof Mention || slot0Mention instanceof ValueMention))
      return new ArrayList<>();
    if (!(slot1Mention instanceof Mention || slot1Mention instanceof ValueMention))
      return new ArrayList<>();

    // Probably never need to connect two ValueMentions. We need to get the sentence theory
    // from one of them so one must be a Mention
    SentenceTheory st;
    if (slot0Mention instanceof Mention)
      st = ((Mention) slot0Mention).sentenceTheory(docTheory);
    else if (slot1Mention instanceof Mention)
      st = ((Mention) slot1Mention).sentenceTheory(docTheory);
    else
      return new ArrayList<>();

    List<Pattern> results = new ArrayList<>();
    for (Proposition startingProp : st.propositions()) {
        results.addAll(generateBinaryPropPatternsGeneral(
            slot0Mention, slot1Mention, startingProp,
            depthConstraint, constraints));
    }
    return results;
  }

  // EventMention, EventMention
  public List<Pattern> generateBinaryPropPatterns(
      EventMention slot0EventMention, EventMention slot1EventMention,
      int depthConstraint, Set<CONSTRAINTS> constraints) {

    SentenceTheory st = slot0EventMention.sentenceTheory(docTheory);
    List<Pattern> results = new ArrayList<>();

    if (!eventMentionToObjectCache.containsKey(slot0EventMention) ||
        !eventMentionToObjectCache.containsKey(slot1EventMention))
      return results;

    // objects are propositions and mentions
    Set<Object> overlappingSlot0Objects = eventMentionToObjectCache.get(slot0EventMention);
    Set<Object> overlappingSlot1Objects = eventMentionToObjectCache.get(slot1EventMention);

    for (Proposition startingProp : st.propositions()) {
      for (Object slot0Object : overlappingSlot0Objects) {
        for(Object slot1Object : overlappingSlot1Objects) {
          if (slot0Object == slot1Object)
            continue;

          results.addAll(generateBinaryPropPatternsGeneral(
              slot0Object, slot1Object, startingProp,
              depthConstraint, constraints));
        }
      }
    }
    return results;
  }

  // EventMention, Mention
  public List<Pattern> generateBinaryPropPatterns(
      EventMention slot0EventMention, Object slot1Mention,
      int depthConstraint, Set<CONSTRAINTS> constraints) {

    if (!(slot1Mention instanceof Mention || slot1Mention instanceof ValueMention))
      return new ArrayList<>();

    SentenceTheory st = slot0EventMention.sentenceTheory(docTheory);
    List<Pattern> results = new ArrayList<>();

    if (!eventMentionToObjectCache.containsKey(slot0EventMention))
      return results;

    // objects are propositions and mentions
    Set<Object> overlappingSlot0Objects = eventMentionToObjectCache.get(slot0EventMention);

    for (Proposition startingProp : st.propositions()) {
      for (Object slot0Object : overlappingSlot0Objects) {
          results.addAll(generateBinaryPropPatternsGeneral(
              slot0Object, slot1Mention, startingProp,
              depthConstraint, constraints));
      }
    }
    return results;
  }

  public List<Pattern> generateBinaryPropPatternsGeneral(
      Object slot0, Object slot1, Proposition startingProposition,
      int depthConstraint, Set<CONSTRAINTS> constraints) {

    if (!isValidStartingProp(startingProposition, slot0, slot1))
      return new ArrayList<>();

    globalConstraints = constraints;

    List<Pattern> results = new ArrayList<>();

    if (slot0 == slot1)
      return results;

    objectsToLabel.clear();
    objectsToLabel.put(slot0, SLOT_0);
    objectsToLabel.put(slot1, SLOT_1);

    List<Pattern> intermediateResults =
        new ArrayList<>(generatePropPatterns(startingProposition, depthConstraint));

    for (Pattern pattern : intermediateResults) {
      Set<Symbol> labels = getLabels((PropPattern) pattern);
      if (foundAllLabels(objectsToLabel.values(), labels)) {
        results.add(pattern);
      }
    }
    return results;
  }

  boolean isValidStartingProp(Proposition p, Object o1, Object o2) {

   if (!reachableObjects.containsKey(p)) return false;

   return
       reachableObjects.get(p).contains(o1) && reachableObjects.get(p).contains(o2);
  }

  boolean isValidStartingProp(Proposition p, Object o) {

    if (!reachableObjects.containsKey(p)) return false;

    return
        reachableObjects.get(p).contains(o);
  }

  public List<Pattern> generateBinaryLexicalPatterns(
      EventMention slot0EventMention, EventMention slot1EventMention,
      int numInBetweenWords, Set<CONSTRAINTS> constraints) {

    return generateBinaryLexicalPatterns(
        slot0EventMention.anchorNode(),
        slot1EventMention.anchorNode(),
        numInBetweenWords, constraints);
  }

  public List<Pattern> generateBinaryLexicalPatterns(
      EventMention slot0EventMention, Mention slot1Mention,
      int numInBetweenWords, Set<CONSTRAINTS> constraints) {

    return generateBinaryLexicalPatterns(
        slot0EventMention.anchorNode(),
        slot1Mention.atomicHead(),
        numInBetweenWords, constraints);
  }

  public List<Pattern> generateBinaryLexicalPatterns(
      Mention slot0Mention, Mention slot1Mention,
      int numInBetweenWords, Set<CONSTRAINTS> constraints) {
    List<Pattern> results = new ArrayList<>();

    globalConstraints = constraints;
    objectsToLabel.clear();
    objectsToLabel.put(slot0Mention, SLOT_0);
    objectsToLabel.put(slot1Mention, SLOT_1);

    results.addAll(
        getLexicalPatterns(
            slot0Mention.atomicHead(), slot1Mention.atomicHead(),
            slot0Mention, slot1Mention, numInBetweenWords));

    return results;
  }

  public List<Pattern> generateBinaryLexicalPatterns(
      SynNode slot0SynNode, SynNode slot1SynNode,
      int numInBetweenWords, Set<CONSTRAINTS> constraints) {
    List<Pattern> results = new ArrayList<>();

    globalConstraints = constraints;
    objectsToLabel.clear();
    // For SynNodes, getLexicalPatterns does not check objectsToLabel, it knows to label
    // slot0SynNode and slot1SynNode

    results.addAll(
        getLexicalPatterns(
            slot0SynNode, slot1SynNode,
            null, null, numInBetweenWords));

    return results;
  }


  public List<Pattern> generateBinaryLexicalPatterns(
      Mention slot0Mention, SynNode slot1SynNode,
      int numInBetweenWords, Set<CONSTRAINTS> constraints) {
    List<Pattern> results = new ArrayList<>();

    globalConstraints = constraints;
    objectsToLabel.clear();
    objectsToLabel.put(slot0Mention, SLOT_0);
    // For SynNodes, getLexicalPatterns does not check objectsToLabel, it knows to label
    // slot0SynNode and slot1SynNode

    results.addAll(
        getLexicalPatterns(
            slot0Mention.atomicHead(), slot1SynNode,
            slot0Mention, null, numInBetweenWords));

    return results;
  }

  public List<Pattern> generateBinaryLexicalPatterns(
      SynNode slot0SynNode, Mention slot1Mention,
      int numInBetweenWords, Set<CONSTRAINTS> constraints) {
    List<Pattern> results = new ArrayList<>();

    globalConstraints = constraints;
    objectsToLabel.clear();
    objectsToLabel.put(slot1Mention, SLOT_1);
    // For SynNodes, getLexicalPatterns does not check objectsToLabel, it knows to label
    // slot0SynNode and slot1SynNode

    results.addAll(
        getLexicalPatterns(
            slot0SynNode, slot1Mention.atomicHead(),
            null, slot1Mention, numInBetweenWords));

    return results;
  }

  // slot0Mention and slot1Mention can be null, if we're just trying to make a lexical pattern
  // between SynNodes
  private List<Pattern> getLexicalPatterns(
      SynNode slot0SynNode, SynNode slot1SynNode,
      Mention slot0Mention, Mention slot1Mention,
      int numInBetweenWords)
  {
    List<Pattern> results = new ArrayList<>();

    SentenceTheory st = slot0SynNode.sentenceTheory(docTheory);

    TokenSpan slot0TokenSpan = slot0SynNode.head().tokenSpan();
    TokenSpan slot1TokenSpan = slot1SynNode.head().tokenSpan();

    int inBetweenWordCount = 0;
    boolean inRange = false;
    boolean complete = false;
    StringBuilder inBetweenString = new StringBuilder();
    List<Pattern> subPatterns = new ArrayList<>();
    for (Token t : st.tokenSpan().tokens(docTheory)) {

      // We hit start of slot0 and we've already seen slot1
      if (inRange && t == slot0TokenSpan.startToken())
      {
        subPatterns.add(getLexicalTextPattern(inBetweenString));
        subPatterns.add(getSubPattern(slot0Mention, "slot0"));
        complete = true;
        break;
      }

      // We hit start of slot1 and we've already seen slot0
      if (inRange && t == slot1TokenSpan.startToken()) {
        subPatterns.add(getLexicalTextPattern(inBetweenString));
        subPatterns.add(getSubPattern(slot1Mention, "slot1"));
        complete = true;
        break;
      }

      // We hit the end of the first slot and haven't seen the second slot
      if (!inRange && t == slot0TokenSpan.endToken()) {
        subPatterns.add(getSubPattern(slot0Mention, "slot0"));
        inRange = true;
        continue;
      }

      // We hit the end of the second slot and haven't seen the first slot
      if (!inRange && t == slot1TokenSpan.endToken()) {
        subPatterns.add(getSubPattern(slot1Mention, "slot1"));
        inRange = true;
        continue;
      }

      if (inBetweenWordCount == numInBetweenWords)
        break;

      if (inRange) {
        inBetweenString.append(t.text());
        inBetweenString.append(" ");
        inBetweenWordCount += 1;
      }
    }

    if (complete) {
      RegexPattern.Builder rpb = new RegexPattern.Builder();
      rpb.withSubpatterns(subPatterns);
      results.add(rpb.build());
    }

    return results;
  }

  private Pattern getLexicalTextPattern(StringBuilder sb) {
    TextPattern.Builder tpb = new TextPattern.Builder();
    String s = sb.toString();
    if (s.endsWith(" ")) {
      s = s.substring(0, s.length() - 1);
    }
    tpb.withText(s);
    tpb.withAddSpaces(true);
    return tpb.build();
  }

  private Pattern getSubPattern(Mention mention, String label) {
    if (mention == null)
      return getTextPattern(label);
    else
      return getMentionPatterns(mention).get(0); // first one is simplest pattern
  }

  private TextPattern getTextPattern(String returnLabel) {
    TextPattern.Builder tpb = new TextPattern.Builder();
    PatternReturn patternReturn = new LabelPatternReturn(Symbol.from(returnLabel));
    tpb.withPatternReturn(patternReturn);
    tpb.withAddSpaces(true);
    return tpb.build();
  }

  // Recurses over PropPattern and returns what return labels are found.
  private Set<Symbol> getLabels(PropPattern pattern) {
    Set<Symbol> results = new HashSet<>();

    checkForPatternReturn(pattern, results);

    for (ArgumentPattern a : pattern.getArgs()) {
      if (a.getPattern() instanceof PropPattern) {
        Set<Symbol> childResults = getLabels((PropPattern) a.getPattern());
        results.addAll(childResults);
      }
      if (a.getPattern() instanceof MentionPattern) {
        checkForPatternReturn(a.getPattern(), results);
      }
      if (a.getPattern() instanceof ValueMentionPattern) {
        checkForPatternReturn(a.getPattern(), results);
      }
    }

    return results;
  }

  // Returns true of all members of found set are in needToFind collection
  // false otherwise
  private boolean foundAllLabels(Collection<Symbol> needToFind, Set<Symbol> found) {
    for (Symbol s : needToFind) {
      if (!(found.contains(s))) return false;
    }
    return true;
  }

  private void checkForPatternReturn(Pattern pattern, Set<Symbol> results) {
    PatternReturn patternReturn = pattern.getPatternReturn();

    if (!(patternReturn instanceof LabelPatternReturn))
      return;

    LabelPatternReturn labelPatternReturn = (LabelPatternReturn) patternReturn;
    results.add(labelPatternReturn.getLabel());
  }

  private List<Pattern> generatePropPatterns(
      Proposition proposition, int depthConstraint)
  {
    maximumDepth = depthConstraint;

    return(new ArrayList<Pattern>(getPropositionPatterns(proposition, 1)));
  }

  private List<Mention> getAllMentionsHeadedOnSynNode(SynNode synNode, SentenceTheory st) {
    List<Mention> results = new ArrayList<>();
    SynNode synNodePreterminal = synNode.headPreterminal();

    for (Mention mention : st.mentions()) {
      if (synNodePreterminal == mention.node().headPreterminal()) {
        results.add(mention);
      }
    }
    return results;
  }

  private List<Proposition> getAllPropositionsFromSynNode(SynNode synNode, SentenceTheory st) {
    List<Proposition> results = new ArrayList<>();
    SynNode synNodePreterminal = synNode.headPreterminal();

    for (Proposition p : st.propositions()) {
      if (p.predHead().isPresent() &&
          p.predHead().get().headPreterminal() == synNodePreterminal)
      {
        results.add(p);
      }
    }
    return results;
  }

  private List<PropPattern> getPropositionPatterns(Proposition p, int currentDepth) {

    List<PropPattern> results = new ArrayList<>();

    if (p.predType() != PredicateType.VERB &&
        p.predType() != PredicateType.MODIFIER &&
        p.predType() != PredicateType.NOUN &&
        p.predType() != PredicateType.SET &&
        p.predType() != PredicateType.COPULA &&
        p.predType() != PredicateType.COMP) {
      return results;
    }

    if (globalConstraints.contains(CONSTRAINTS.NO_LIST_PROPS) &&
        (p.predType() == PredicateType.SET || p.predType() == PredicateType.COMP))
      return results;

    Set<Argument> argumentSet = new HashSet<>();
    for (int i = 0; i < p.numArgs(); i++) {
      argumentSet.add(p.arg(i));
    }

    int argLimit = 3;
    // These are "list-y" props, too many args can explode the number of possibilities
    if (p.predType() != PredicateType.SET || p.predType() != PredicateType.COMP)
      argLimit = 2;
    Set<Set<Argument>> argumentPowerSet = Sets.powerSet(argumentSet);
    if (globalConstraints.contains(CONSTRAINTS.LIMIT_ARGS_TO_1))
      argLimit = 1;
    else if (globalConstraints.contains(CONSTRAINTS.LIMIT_ARGS_TO_2))
      argLimit = 2;

    Set<Set<Argument>> prunedArgSet = new HashSet<>();
    for (Set<Argument> set : argumentPowerSet) {
      if (set.size() <= argLimit)
        prunedArgSet.add(set);
    }
    argumentPowerSet = prunedArgSet;

    Set<Symbol> predicates = getPredicates(p);
    // For each set of arguments, we'll make a new set of PropPatterns
    for (Set<Argument> argumentSubset : argumentPowerSet) {

      PredicateType predType = p.predType();
      if (predType == PredicateType.COPULA) {
        predType = PredicateType.VERB;
      }

      List<PropPattern.Builder> patternBuilders = new ArrayList<>();
      PatternReturn patternReturn = null;
      if (objectsToLabel.containsKey(p)) {
        patternReturn = new LabelPatternReturn(objectsToLabel.get(p));
      }
      PropPattern.Builder noPredicatesBuilder = new PropPattern.Builder(predType);
      noPredicatesBuilder.withPatternReturn(patternReturn);
      patternBuilders.add(noPredicatesBuilder);

      // If predicate head constraint is set, we make two prop pattern builders -- one without
      // predicates (above), and one with predicates
      if ((objectsToLabel.containsKey(p) && globalConstraints.contains(CONSTRAINTS.SLOT_PREDICATE_HEAD)) ||
          (!objectsToLabel.containsKey(p) && globalConstraints.contains(CONSTRAINTS.PREDICATE_HEAD)))
      {
        PropPattern.Builder withPredicateBuilder = new PropPattern.Builder(predType);
        withPredicateBuilder.withPatternReturn(patternReturn);
        withPredicateBuilder.withPredicates(predicates);
        patternBuilders.add(withPredicateBuilder);
      }

      List<List<ArgumentPattern>> argumentPatternsList =
          getArgumentPatterns(argumentSubset, currentDepth);

      for (PropPattern.Builder patternBuilder : patternBuilders) {
        for (List<ArgumentPattern> argumentPatterns : argumentPatternsList) {
          patternBuilder.withArgs(argumentPatterns);
          PropPattern result = patternBuilder.build();
          results.add(result);
        }
      }
    }
    return results;
  }

  private List<List<ArgumentPattern>> getArgumentPatterns(
      Set<Argument> argumentSet, int currentDepth) {
    List<List<ArgumentPattern>> results = new ArrayList<>();

    // For each argument, create a set of possible ArgumentPattern objects.

    List<Set<ArgumentPattern>> listOfSets = new ArrayList<>();
    for (Argument a : argumentSet) {

      if (a instanceof MentionArgument) {
        Mention m = ((MentionArgument) a).mention();
        if (globalConstraints.contains(CONSTRAINTS.MENTION_ARGUMENT) ||
            objectsToLabel.containsKey(m))
        {
          HashSet<ArgumentPattern> as = new HashSet<>();
          ArgumentPattern.Builder argPatternBuilder = new ArgumentPattern.Builder();
          if (globalConstraints.contains(CONSTRAINTS.ARGUMENT_ROLE)) {
            List<Symbol> roles = getRoles(a);
            argPatternBuilder.withRoles(roles);
          }

          List<MentionPattern> mentionPatterns = getMentionPatterns(m);
          for (MentionPattern mentionPattern : mentionPatterns) {
            argPatternBuilder.withPattern(mentionPattern);
            if (globalConstraints.contains(CONSTRAINTS.PRUNE_SIMPLE_REFS) &&
                a.role().get() == Symbol.from("<ref>") &&
                isSimpleReferent(mentionPattern))
            {
              continue;
            }
            as.add(argPatternBuilder.build());
          }

          // Dive into list mention
          if (m.mentionType() == Mention.Type.LIST) {
            Optional<Mention> child = m.child();
            while (child.isPresent()) {
              List<MentionPattern> childMentionPatterns = getMentionPatterns(child.get());
              for (MentionPattern childMentionPattern : childMentionPatterns) {
                argPatternBuilder.withPattern(childMentionPattern);
                as.add(argPatternBuilder.build());
              }
              child = child.get().next();
            }
          }
          listOfSets.add(as);
        }
      }

      // Look for match with ValueMention, we don't have ValueMentionArguments like we have
      // Mention arguments, so look for a MentionArgument with type OTH whose head overlaps with
      // the ValueMention in question
      if (a instanceof MentionArgument &&
          ((MentionArgument) a).mention().entityType().name() == Symbol.from("OTH")) {

        if (globalConstraints.contains(CONSTRAINTS.VALUE_MENTION_ARGUMENT) ||
            objectsToLabelContainsAnyValueMention()) {

          /// Need to look for ValueMention match
          List<ValueMention> valueMentions =
              getOverlappingValueMentions(((MentionArgument) a).mention());

          HashSet<ArgumentPattern> as = new HashSet<>();

          for (ValueMention valueMention : valueMentions) {
            ArgumentPattern.Builder argPatternBuilder = new ArgumentPattern.Builder();
            if (globalConstraints.contains(CONSTRAINTS.ARGUMENT_ROLE)) {
              List<Symbol> roles = getRoles(a);
              argPatternBuilder.withRoles(roles);
            }

            if (objectsToLabel.containsKey(valueMention) ||
                globalConstraints.contains(CONSTRAINTS.VALUE_MENTION_ARGUMENT))
            {
              List<ValueMentionPattern> valueMentionPatterns =
                  getValueMentionPatterns(valueMention);
              for (ValueMentionPattern valueMentionPattern : valueMentionPatterns) {
                argPatternBuilder.withPattern(valueMentionPattern);
                as.add(argPatternBuilder.build());
              }
            }
          }
          if (as.size() > 0)
            listOfSets.add(as);
        }
      }

      if (a instanceof PropositionArgument &&
          globalConstraints.contains(CONSTRAINTS.PROPOSITION_ARGUMENT) &&
          currentDepth < maximumDepth)
      {
        HashSet<ArgumentPattern> as = new HashSet<>();
        List<PropPattern> propPatterns =
            getPropositionPatterns(
                ((PropositionArgument) a).proposition(), currentDepth + 1);
        for (PropPattern pp : propPatterns) {
          ArgumentPattern.Builder argPatternBuilder = new ArgumentPattern.Builder();
          if (globalConstraints.contains(CONSTRAINTS.ARGUMENT_ROLE)) {
            List<Symbol> roles = getRoles(a);
            argPatternBuilder.withRoles(roles);
          }
          argPatternBuilder.withPattern(pp);
          as.add(argPatternBuilder.build());
        }
        listOfSets.add(as);
      }

    }

    // generate all possibilities of combination of ArgumentPatterns
    Set<List<ArgumentPattern>> crossProduct = Sets.cartesianProduct(listOfSets);
    results.addAll(crossProduct);

    return results;
  }

  private boolean objectsToLabelContainsAnyValueMention() {
    for (Object o : objectsToLabel.keySet()) {
      if (o instanceof ValueMention)
        return true;
    }
    return false;
  }

  private List<ValueMention> getOverlappingValueMentions(Mention m) {
    List<ValueMention> valueMentions = new ArrayList<>();

    SentenceTheory st = m.sentenceTheory(docTheory);
    for (ValueMention vm : st.valueMentions()) {
      if (vm.tokenSpan().contains(m.atomicHead().span())) {
        valueMentions.add(vm);
      }
    }
    return valueMentions;
  }

  private List<MentionPattern> getMentionPatterns(Mention m) {
    List<MentionPattern> results = new ArrayList<>();

    List<EntityType> entityTypes = new ArrayList<>();
    entityTypes.add(m.entityType());

    Set<Symbol> headwords = new HashSet<>();
    headwords.add(m.head().headWord());

    List<MentionPattern.Builder> mentionPatternBuilders = new ArrayList<>();

    LabelPatternReturn patternReturn = null;
    if (objectsToLabel.containsKey(m))
      patternReturn = new LabelPatternReturn(objectsToLabel.get(m));

    // Expand to all possibilities with respect to entity types and headwords
    if (objectsToLabel.containsKey(m)) {
      MentionPattern.Builder noConstraintsBuilder = new MentionPattern.Builder();
      noConstraintsBuilder.withPatternReturn(patternReturn);
      mentionPatternBuilders.add(noConstraintsBuilder);

      if (globalConstraints.contains(CONSTRAINTS.SLOT_MENTION_ENTITY_TYPE)) {
        // Just entity type
        MentionPattern.Builder entityTypeBuilder = new MentionPattern.Builder();
        entityTypeBuilder.withPatternReturn(patternReturn);
        entityTypeBuilder.withAceTypes(entityTypes);
        mentionPatternBuilders.add(entityTypeBuilder);
      }

      if (globalConstraints.contains(CONSTRAINTS.SLOT_MENTION_HEAD_WORD)) {
        // Just head word
        MentionPattern.Builder headWordBuilder = new MentionPattern.Builder();
        headWordBuilder.withPatternReturn(patternReturn);
        headWordBuilder.withHeadwords(headwords);
        mentionPatternBuilders.add(headWordBuilder);
      }

      if (globalConstraints.contains(CONSTRAINTS.SLOT_MENTION_ENTITY_TYPE) &&
          globalConstraints.contains(CONSTRAINTS.SLOT_MENTION_HEAD_WORD))
      {
        // both
        MentionPattern.Builder bothBuilder = new MentionPattern.Builder();
        bothBuilder.withPatternReturn(patternReturn);
        bothBuilder.withAceTypes(entityTypes);
        bothBuilder.withHeadwords(headwords);
        mentionPatternBuilders.add(bothBuilder);
      }
    } else { // !objectsToLabel.containsKey(m))
      MentionPattern.Builder noConstraintsBuilder = new MentionPattern.Builder();
      mentionPatternBuilders.add(noConstraintsBuilder);

      if (globalConstraints.contains(CONSTRAINTS.MENTION_ENTITY_TYPE)) {
        // Just entity type
        MentionPattern.Builder entityTypeBuilder = new MentionPattern.Builder();
        entityTypeBuilder.withAceTypes(entityTypes);
        mentionPatternBuilders.add(entityTypeBuilder);
      }

      if (globalConstraints.contains(CONSTRAINTS.MENTION_HEAD_WORD)) {
        // Just head word
        MentionPattern.Builder headWordBuilder = new MentionPattern.Builder();
        headWordBuilder.withHeadwords(headwords);
        mentionPatternBuilders.add(headWordBuilder);
      }

      if (globalConstraints.contains(CONSTRAINTS.MENTION_ENTITY_TYPE) &&
          globalConstraints.contains(CONSTRAINTS.MENTION_HEAD_WORD))
      {
        // both
        MentionPattern.Builder bothBuilder = new MentionPattern.Builder();
        bothBuilder.withAceTypes(entityTypes);
        bothBuilder.withHeadwords(headwords);
        mentionPatternBuilders.add(bothBuilder);
      }
    }

    for (MentionPattern.Builder mentionPatternBuilder : mentionPatternBuilders) {
      MentionPattern mp = mentionPatternBuilder.build();
      if (isMeaningfulMentionPattern(mp))
        results.add(mp);

      if (globalConstraints.contains(CONSTRAINTS.MENTION_PREMOD)) {
        Set<String> premods = new HashSet<>();
        Token headToken = m.head().tokenSpan().startToken();
        StringBuilder fullPremodStringBuilder = new StringBuilder();
        int token_count = 0;
        for (Token t : m.tokenSpan().tokens(docTheory)) {
          if (t == headToken)
            break;
          String tokenString = t.rawOriginalText().toString().toLowerCase();
          if (token_count != 0)
            fullPremodStringBuilder.append(" ");
          fullPremodStringBuilder.append(tokenString);
          token_count++;
          if (!mentionPremodStopWords.contains(tokenString))
            premods.add(tokenString);
        }
        for (String premod : premods) {
          mentionPatternBuilder.withRegexPattern(getRegexPatternFromString(premod));
          MentionPattern mentionPatternWithPremods = mentionPatternBuilder.build();
          if (mentionPatternWithPremods.getHeadwords().size() > 0)
            results.add(mentionPatternWithPremods);
        }
        if (premods.size() > 1) {
          mentionPatternBuilder.withRegexPattern(
              getRegexPatternFromString(fullPremodStringBuilder.toString()));
          MentionPattern mentionPatternWithPremods = mentionPatternBuilder.build();
          if (mentionPatternWithPremods.getHeadwords().size() > 0)
            results.add(mentionPatternWithPremods);
        }
      }
    }

    return results;
  }

  public static RegexPattern getRegexPatternFromString(String s) {
    TextPattern.Builder textPatternBuilder = new TextPattern.Builder();
    textPatternBuilder.withText(s);
    textPatternBuilder.withAddSpaces(true);
    List<Pattern> regexSubpatterns = new ArrayList<>();
    regexSubpatterns.add(textPatternBuilder.build());

    RegexPattern.Builder regexPatternBuilder = new RegexPattern.Builder();
    regexPatternBuilder.withSubpatterns(regexSubpatterns);
    regexPatternBuilder.withAddSpaces(true);
    return regexPatternBuilder.build();
  }

  private List<ValueMentionPattern> getValueMentionPatterns(ValueMention vm) {
    List<ValueMentionPattern> results = new ArrayList<>();

    ValueMentionPattern.Builder valueMentionPatternBuilder = new ValueMentionPattern.Builder();

    if (objectsToLabel.containsKey(vm)) {
      PatternReturn patternReturn = new LabelPatternReturn(objectsToLabel.get(vm));
      valueMentionPatternBuilder.withPatternReturn(patternReturn);
    }

    // No value type
    results.add(valueMentionPatternBuilder.build());

    if (globalConstraints.contains(CONSTRAINTS.VALUE_MENTION_VALUE_TYPE)) {
      List<ValueType> valueTypes = new ArrayList<>();
      valueTypes.add(vm.fullType());
      valueMentionPatternBuilder.withValueTypes(valueTypes);
      results.add(valueMentionPatternBuilder.build());
    }

    return results;
  }

  private List<Symbol> getRoles(Argument a) {
    List<Symbol> roles = new ArrayList<>();
    roles.add(a.role().get());
    return roles;
  }

  private Set<Symbol> getPredicates(Proposition p) {
    Set<Symbol> predicates = new HashSet<>();
    if (p.predHead().isPresent())
      predicates.add(p.predHead().get().headWord());
    return predicates;
  }

  private boolean isSimpleReferent(MentionPattern mp) {
    return (mp.getEntityTypes().size() == 0 &&
            mp.getRegexPattern() == null);
  }

  private boolean isMeaningfulMentionPattern(MentionPattern mp) {
    return mp.getHeadwords().size() > 0 ||
           mp.getRegexPattern() != null ||
           mp.getEntityTypes().size() > 0 ||
           mp.getPatternReturn() != null;
  }

}
