package com.bbn.serif.theories;

import com.bbn.bue.common.StringUtilsInternal;
import com.bbn.bue.common.UnicodeFriendlyString;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import static com.bbn.serif.theories.Spannings.tokenizedTextFunction;
import static com.google.common.collect.Iterables.transform;

/**
 * Utility methods for use with {@link TokenSequence}s.
 */
public final class TokenSequences {

  private TokenSequences() {
    throw new UnsupportedOperationException();
  }

  public static TokenSequenceCaseDetector caseDetector() {
    return DefaultTokenSequenceCaseDetectorImpl.INSTANCE;
  }
}


enum DefaultTokenSequenceCaseDetectorImpl implements TokenSequenceCaseDetector {
  INSTANCE;

  private static final Joiner CONCAT_JOINER = Joiner.on("");

  @Override
  public SentenceCasing detectCasing(TokenSequence ts) {
    final String concatenatedTokens = FluentIterable.from(ts)
        .transform(tokenizedTextFunction())
        .join(CONCAT_JOINER);

    return new SentenceCasing.Builder()
        .forAllCharacters(StringUtilsInternal.countLetters(concatenatedTokens))
        .tokenInitial(StringUtilsInternal.countLetters(tokenInitialCharacters(ts)))
        .build();
  }

  private String tokenInitialCharacters(TokenSequence ts) {
    final StringBuilder ret = new StringBuilder();
    for (final UnicodeFriendlyString tok : transform(ts, tokenizedTextFunction())) {
      if (!tok.isEmpty()) {
        ret.appendCodePoint(tok.utf16CodeUnits().codePointAt(0));
      }
    }
    return ret.toString();
  }
}
