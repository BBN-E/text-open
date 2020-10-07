package com.bbn.serif.tokens;

import com.bbn.bue.common.Finishable;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Document;
import com.bbn.serif.theories.Sentence;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.tokens.constraints.TokenizationConstraint;

import com.google.common.annotations.Beta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import javax.inject.Qualifier;

/**
 * Tokenizes a Sentence given the original sentence text, subject to a set of constraints, see
 * {@code TokenizationConstaint} for more details. The implementation is required to verify that the
 * constraints are obeyed or warn or crash as appropriate.
 */
@Beta
public interface TokenFinder extends Finishable {

  DocTheory tokenize(final DocTheory docTheory);

  DocTheory tokenize(final DocTheory docTheory, final Set<TokenizationConstraint> constraints);

  SentenceTheory tokenize(final Document document,
      final Sentence sentence, final Set<TokenizationConstraint> constraints);

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TolerantP {

    String param = "com.bbn.serif.tokens.jacserif.tolerant";
  }
}


