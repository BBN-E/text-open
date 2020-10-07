package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;

import org.immutables.func.Functional;

/**
 * A form used as a {@link MorphTokenAnalysis#roots()} or {@link MorphTokenAnalysis#lemmas()}.
 */
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@JsonSerialize
@JsonDeserialize
@Functional
public abstract class LexicalForm {
  /**
   * The string associated with this form. e.g. "dog"
   */
  public abstract Symbol form();

  /**
   * Whether or not this form appears in the lexicon (if any) used by the morphological analyzer.
   */
  @org.immutables.value.Value.Default
  public boolean inLexicon() {
    return false;
  }

  /**
   * The meanings of this form.
   */
  public abstract ImmutableList<Gloss> glosses();

  public static class Builder extends ImmutableLexicalForm.Builder {}

  @Override
  public String toString() {
    String ret = form().asString();
    if (inLexicon()) {
      ret = ret + "[inLex]";
    }
    if (!glosses().isEmpty()) {
      ret = ret + "[" + glosses().size() + " glosses]";
    }
    return ret;
  }
}
