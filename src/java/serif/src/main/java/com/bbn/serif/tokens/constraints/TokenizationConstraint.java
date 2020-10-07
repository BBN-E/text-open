package com.bbn.serif.tokens.constraints;


import com.bbn.bue.common.strings.LocatedString;
import com.bbn.serif.theories.Token;

import com.google.common.annotations.Beta;

/**
 * Determines whether or not a Tokenization is valid for this String. Various implementations exist.
 * The consumer for this interface is obligated to call the {@code satisfyBy} method.
 */
@Beta
public interface TokenizationConstraint {

  boolean satisfiedBy(final LocatedString sentence, final Iterable<Token> sequence);
}
