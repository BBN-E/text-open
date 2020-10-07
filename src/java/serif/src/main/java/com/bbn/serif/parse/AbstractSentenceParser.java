package com.bbn.serif.parse;

import com.bbn.serif.common.SerifException;
import com.bbn.serif.parse.constraints.ParseConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Parse;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.TokenSequence;

import com.google.common.annotations.Beta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@Beta
public abstract class AbstractSentenceParser implements Parser {

  private static final Logger log = LoggerFactory.getLogger(AbstractSentenceParser.class);

  private final boolean tolerant;
  protected int failed = 0;
  protected int successful = 0;

  protected AbstractSentenceParser(final boolean tolerant) {
    this.tolerant = tolerant;
  }

  @Override
  public final DocTheory addParse(final DocTheory input,
      final Set<ParseConstraint> parseConstraints) {
    final DocTheory.Builder ret = input.modifiedCopyBuilder();
    for (int i = 0; i < input.numSentences(); i++) {
      final SentenceTheory st = input.sentenceTheory(i);
      final TokenSequence tokenSequence = st.tokenSequence();
      final SentenceTheory.Builder retSt = st.modifiedCopyBuilder();

      // TODO isolate ParseConstraints to this sentence
      retSt.parse(parseForSentence(tokenSequence, parseConstraints));
      ret.replacePrimarySentenceTheory(st, retSt.build());
    }

    final DocTheory output = ret.build();
    for (final ParseConstraint cons : parseConstraints) {
      final boolean satisfied = cons.satisfiedBy(output);
      if (!satisfied) {
        failed++;
        if (tolerant) {
          log.warn("Failed to enforce constraint {} in {}", cons, output.docid());
        } else {
          throw new SerifException(
              "Failed to enforce constraint " + cons + " in " + output.docid());
        }
      } else {
        successful++;
      }
    }

    return ret.build();
  }

  protected abstract Parse parseForSentence(final TokenSequence tokenSequence,
      final Set<ParseConstraint> parseConstraints);

}
