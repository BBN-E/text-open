package com.bbn.serif.languages;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.languages.Spanish;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

public class SpanishSerifLanguage extends SerifLanguage {
  @Inject
  private SpanishSerifLanguage(@JsonProperty("language") Spanish spanish) {
    super(spanish);
  }

  private static final SpanishSerifLanguage INSTANCE = new SpanishSerifLanguage(Spanish.getInstance());

  public static SpanishSerifLanguage getInstance() {
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
        if (isNoun(curNode)) {
            while (curNode.parent().isPresent()
                    && curNode.parent().get().head().equals(curNode)
                    && isNoun(curNode.parent().get())) {
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
        return n.tag().equalTo(CD) || n.tag().equalTo(OD);
    }

    @Override
    public boolean isQuestion(SentenceTheory st) {
        return st.span().endToken().symbol().equalTo(QUESTION_MARK);
    }


    @Override
    /**
     * Heuristic approach:  returns true iff the mention's head word is a common noun that ends in 's'.
     */
    public boolean isPlural(Mention m){
      return language().isNominalPOSTag(m.head().headPOS()) && !((Spanish) language())
          .isProperNoun(m.head().headPOS()) && m.head().headWord().asString().endsWith("s");
    }

  public static final class Module extends AbstractModule {
    @Override
    protected void configure() {
      install(new Spanish.Module());
    }
  }

}
