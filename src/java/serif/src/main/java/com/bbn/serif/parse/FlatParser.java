package com.bbn.serif.parse;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.parse.constraints.ParseConstraint;
import com.bbn.serif.theories.Parse;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import java.util.Set;

import javax.inject.Inject;

/**
 * Provides a flat/dummy parse for a given sentence theory. Does not provide any semantic
 * information.
 */
@Beta
public final class FlatParser extends AbstractSentenceParser {

  @Inject
  private FlatParser(@TolerantP final boolean tolerant) {
    super(tolerant);
  }

  @Override
  protected Parse parseForSentence(final TokenSequence tokenSequence,
      final Set<ParseConstraint> parseConstraints) {
    if (ImmutableList.copyOf(parseConstraints).size() > 0) {
      throw new RuntimeException("FlatParser does not know how to handle constraints");
    }
    final SynNode.NonterminalBuilder fake = SynNode.nonterminalBuilder(Symbol.from("X"));
    final SynNode headTerminal = SynNode.nonterminalBuilder(Symbol.from("X")).appendHead(
        SynNode.terminalBuilder(Symbol.from("X")).tokenIndex(0).build(tokenSequence))
        .build(tokenSequence);
    fake.appendHead(headTerminal);
    for (int i = 1; i < tokenSequence.size(); i++) {
      final SynNode.TerminalBuilder terminalBuilder = SynNode.terminalBuilder(Symbol.from("X"));
      final SynNode.Builder nonterminalBuilder =
          SynNode.nonterminalBuilder(Symbol.from("X"))
              .appendHead(terminalBuilder.tokenIndex(i).build(tokenSequence));

      fake.appendNonHead(nonterminalBuilder.build(tokenSequence));
    }
    return Parse.create(tokenSequence, fake.build(tokenSequence), 0.0f);
  }

  @Override
  public void finish() {

  }

}
