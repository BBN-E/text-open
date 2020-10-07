package com.bbn.serif.languages;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.languages.Chinese;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

public class ChineseSerifLanguage extends SerifLanguage {
  @Inject
  private ChineseSerifLanguage(@JsonProperty("language") Chinese chinese) {
    super(chinese);
  }

  private static final ChineseSerifLanguage INSTANCE = new ChineseSerifLanguage(Chinese.getInstance());

  public static ChineseSerifLanguage getInstance() {
    return INSTANCE;
  }

  private static final Symbol PP = Symbol.from("PP");
  private static final Symbol DT = Symbol.from("DT");
  private static final Symbol CD = Symbol.from("CD");
  private static final Symbol OD = Symbol.from("OD");
  private static final Symbol QUESTION_MARK = Symbol.from("？");

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
          && curNode.parent().get().head().equals(curNode)
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

  @Override
  public boolean isNoun(SynNode node) {
    return language().isNominalPOSTag(node.tag());
  }

  @Override
  public boolean isPrepositionalPhrase(SynNode node) {
    return node.tag() == PP;
  }

  @Override
  public boolean isPreposition(SynNode node) {
    return language().isPrepPOS(node.tag());
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
  public boolean preterminalIsNumber(SynNode n) {
    checkArgument(n.isPreterminal());
    return n.tag().equals(CD) || n.tag().equals(OD);
  }

  @Override
  public boolean isQuestion(SentenceTheory st) {
    return st.span().endToken().symbol().equalTo(QUESTION_MARK);
  }

  // TODO
  // There is no simple way to check for plural in Chinese. There is no POS-tag for plural noun.
  // The language classes should be refactored to do
  // Optional<PluralityDetector> getPluralityDetector()
  @Override
  public boolean isPlural(Mention m) {
    return false;
  }

  public static final class Module extends AbstractModule {

    @Override
    protected void configure() {
      install(new Chinese.Module());
    }
  }
}

