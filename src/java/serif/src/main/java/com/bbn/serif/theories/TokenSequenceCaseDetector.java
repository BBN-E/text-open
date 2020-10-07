package com.bbn.serif.theories;

import com.bbn.bue.common.StringUtilsInternal;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import static com.bbn.serif.theories.Spannings.tokenizedTextFunction;
import static com.google.common.collect.Iterables.transform;

/**
 * Detects the casing patterns of a token sequence.
 */
public interface TokenSequenceCaseDetector {

  SentenceCasing detectCasing(TokenSequence ts);
}


