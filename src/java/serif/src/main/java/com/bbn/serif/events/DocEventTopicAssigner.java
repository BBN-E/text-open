package com.bbn.serif.events;

import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.serialization.jackson.ImmutableMultimapProxy;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpList;
import com.bbn.bue.sexp.SexpReader;
import com.bbn.bue.sexp.SexpUtils;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Corresponds to assignTopic() in Generic/docRelationsEvents/DocEventHandler.cpp
 *
 * @author pshapiro, rgabbard
 */
public final class DocEventTopicAssigner {
  private final ImmutableMultimap<Symbol, Symbol> wordToTopic;
  @JsonProperty("tieBreakOrder")
  private final ImmutableList<Symbol> tieBreakOrder;

  /**
   * Reads a sexp file eventTopicSet and extracts a list of topics and topic wordsets Corresponds to
   * <code>DocEventHandler::initializeDocumentTopics()</code>
   */
  private DocEventTopicAssigner(
      final Multimap<Symbol, Symbol> wordToTopic,
      final List<Symbol> tieBreakOrder) {
    checkArgument(
        ImmutableSet.copyOf(wordToTopic.values()).equals(ImmutableSet.copyOf(tieBreakOrder)));

    this.wordToTopic = ImmutableMultimap.copyOf(wordToTopic);
    this.tieBreakOrder = ImmutableList.copyOf(tieBreakOrder);
  }

  public static DocEventTopicAssigner createDummyForTesting() {
    return new DocEventTopicAssigner(ImmutableMultimap.of(Symbol.from("foo"), Symbol.from("first"),
        Symbol.from("bar"), Symbol.from("second"), Symbol.from("foo"), Symbol.from("second")),
        SymbolUtils.listFrom(ImmutableList.of("first", "second")));
  }

  public static DocEventTopicAssigner from(CharSource source) throws IOException {
  // Reads sexp file
    final Sexp sexp = SexpReader.createDefault().read(source);

    // Gets list of contained sexps
    final SexpList sexpList = SexpUtils.forceList(sexp);
    final List<Symbol> topics = Lists.newArrayList();

    // Gets word set as list of Symbols from the Sexp topic, then maps it to the Symbol topic
    final ImmutableMultimap.Builder<Symbol, Symbol> wordsToTopics = ImmutableMultimap.builder();
    for (int i = 0; i < sexpList.size(); i++) {
      final Symbol topic = SexpUtils.getSexpType(sexpList.get(i));
      topics.add(topic);
      final Sexp topicWordSet = SexpUtils.getSexpArgs(sexpList.get(i));
      for (final Symbol word : SexpUtils.getCheckSymList(topicWordSet)) {
        wordsToTopics.put(word, topic);
      }
    }

    return new DocEventTopicAssigner(wordsToTopics.build(), topics);
  }

  /**
   * Gets parameters from parameter file, constructs <code>DocEventTopicAssigner</code> with given
   * parameters Corresponds to calls to C++ <code>ParamReader</code> Currently ignoring
   * <code>_topics_use_only_nouns parameter</code> in C++ code
   */
  public static DocEventTopicAssigner fromParams(final Parameters params) throws IOException {
    return from(Files.asCharSource(params.getExistingFile("event_topic_set"), Charsets.UTF_8));
  }

  /**
   * Corresponds to DocEventHandler::assignTopic(const DocTheory* docTheory) Chooses best topic,
   * returns <code>Optional<Symbol></code> containing the best topic (whereas the C++ code returns
   * null if there is no best topic, we return Optional.absent())
   */
  public Optional<Symbol> assignTopic(final DocTheory docTheory) {
    final Multiset<Symbol> topicWordCounts = HashMultiset.create();

    int totalTokens = 0;
    // Iterate through sentences, increment the corresponding word count for each topic in our list
    for (final SentenceTheory sentence : docTheory.nonEmptySentenceTheories()) {
      for (final Token token : sentence.tokenSequence()) {
        totalTokens++;
        topicWordCounts.addAll(wordToTopic.get(token.symbol()));
      }
    }

    Symbol bestTopic = null;
    int mostWords = 0;
    for (final Symbol topic : tieBreakOrder) {
      final int count = topicWordCounts.count(topic);

      if (count > mostWords) {
        bestTopic = topic;
        mostWords = count;
      }
    }

    if (bestTopic != null) {
      final double percentage = ((double) mostWords) / totalTokens;
      if (percentage > 0.01) {
        return Optional.fromNullable(bestTopic);
      }
    }

    // If we haven't returned a bestTopic yet, return Optional.absent()
    return Optional.absent();
  }


  @JsonProperty("wordToTopic")
  @SuppressWarnings("deprecation")
  private ImmutableMultimapProxy<Symbol, Symbol> getWordToTopicsForSerialization() {
    return ImmutableMultimapProxy.forMultimap(wordToTopic);
  }

  // serialization word-arounds
  @JsonCreator
  @SuppressWarnings("deprecation")
  static DocEventTopicAssigner fromJson(
      @JsonProperty("wordToTopic")
      final ImmutableMultimapProxy<Symbol, Symbol> wordToTopic,
      @JsonProperty("tieBreakOrder")
      final List<Symbol> tieBreakOrder) {
    return new DocEventTopicAssigner(wordToTopic.toImmutableMultimap(), tieBreakOrder);
  }

  public static final class Module extends AbstractModule {

    @Override
    public void configure() {
    }

    private static final String[] paramNames =
        {"event_topic_set", "com.bbn.serif.events.event_topic_set"};

    @Provides
    @Singleton
    public DocEventTopicAssigner getDocEventTopicAssigner(Parameters params) throws IOException {
      return from(
          Files.asCharSource(
              params.getExistingFile(params.getFirstExistingParamName(paramNames)),
              Charsets.UTF_8));
    }
  }
}
