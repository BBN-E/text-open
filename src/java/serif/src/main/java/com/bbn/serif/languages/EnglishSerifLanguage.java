package com.bbn.serif.languages;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.nlp.languages.English;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

public final class EnglishSerifLanguage extends SerifLanguage {
  @Inject
  private EnglishSerifLanguage(@JsonProperty("language") English english) {
    super(english);
  }

  private static final EnglishSerifLanguage INSTANCE =
      new EnglishSerifLanguage(English.getInstance());

  public static EnglishSerifLanguage getInstance() {
    return INSTANCE;
  }

  private static final Symbol QUESTION_MARK = Symbol.from("?");

  private static final Symbol PP = Symbol.from("PP");

  private static final Symbol IN = Symbol.from("IN");
  private static final Symbol TO = Symbol.from("TO");
  private static final Symbol DT = Symbol.from("DT");
  private static final Symbol NNS = Symbol.from("NNS");
  private static final Symbol NNPS = Symbol.from("NNPS");
  private static final Symbol CD = Symbol.from("CD");

  private static final ImmutableSet<Symbol> PREPOSITIONS = ImmutableSet.of(IN, TO);

  @Override
  public boolean isVerbExcludingModals(SynNode node) {
    return language().isVerbalPOSExcludingModals(node.tag());
  }

  @Override
  public boolean isModalVerb(SynNode node) {
    return node.tag() == English.MD;
  }

  @Override
  public boolean canBeAuxillaryVerb(SynNode node) {
    if (isModalVerb(node)) {
      return true;
    }

    //noinspection SimplifiableIfStatement
    if (isCopula(node)) {
      return true;
    }

    return language().wordCanBeAuxilliaryVerb(node.headWord());
  }

  @Override
  public boolean isCopula(SynNode node) {
    return language().wordIsCopula(node.headWord());
  }

  @Override
  public boolean isNoun(SynNode node) {
    return language().isNominalPOSTag(node.tag());
  }

  @Override
  public boolean isAdverb(SynNode node) {
    return language().isAdverbialPOS(node.tag());
  }

  @Override
  public boolean isParticle(SynNode node) {
    return language().isParticlePOS(node.tag());
  }

  @Override
  public boolean isPrepositionalPhrase(SynNode node) {
    return node.tag() == PP;
  }

  @Override
  public boolean isPreposition(SynNode node) {
    return PREPOSITIONS.contains(node.tag());
  }

  @Override
  public Optional<Symbol> getDeterminer(SynNode n) {
    final Optional<SynNode> firstPreterm = n.firstPreterminal();
    if (firstPreterm.isPresent()) {
      if (firstPreterm.get().tag() == DT) {
        return Optional.of(firstPreterm.get().headWord());
      }
    }
    return Optional.absent();
  }

  @Override
  public Optional<Symbol> findDeterminerForNoun(SynNode curNode) {
    if (curNode.tag().toString().startsWith("N")) {
      while (curNode.parent().isPresent()
          && curNode.parent().get().head() == curNode
          && curNode.parent().get().tag().toString().startsWith("N")) {
        final Optional<Symbol> det = getDeterminer(curNode);
        if (det.isPresent()) {
          return det;
        }
        curNode = curNode.parent().get();
      }
      final Optional<Symbol> det = getDeterminer(curNode);
      if (det.isPresent()) {
        return det;
      }
    }
    return Optional.absent();
  }

  private static final ImmutableSet<Symbol> PLURAL_PRONOUNS = SymbolUtils.setFrom("their", "they",
      "them", "y'all", "yall");
  private static final ImmutableSet<Symbol> PLURAL_NOUN_TAGS = ImmutableSet.of(NNS, NNPS);

  @Override
  public boolean isPlural(Mention m) {
    final boolean isPluralPronoun = m.isPronoun() && PLURAL_PRONOUNS.contains(m.head().headWord());
    final boolean isPluralOtherNoun = PLURAL_NOUN_TAGS.contains(m.head().headPreterminal().tag());

    return isPluralPronoun || isPluralOtherNoun;
  }

  @Override
  public boolean preterminalIsNumber(SynNode n) {
    checkArgument(n.isPreterminal());
    return n.tag().equalTo(CD);
  }

  @Override
  public boolean isQuestion(SentenceTheory st) {
    return st.span().endToken().symbol().equalTo(QUESTION_MARK);
  }

  public static final class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new English.Module());
    }
  }
}
