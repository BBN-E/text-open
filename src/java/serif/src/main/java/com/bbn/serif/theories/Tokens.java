package com.bbn.serif.theories;

import com.bbn.bue.common.CodepointMatcher;
import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

public final class Tokens {

  private Tokens() {
    throw new UnsupportedOperationException();
  }

  public static Function<Token, CharOffset> ToStartCharOffset() {
    return new Function<Token, CharOffset>() {
      @Override
      public CharOffset apply(Token tok) {
        return tok.startCharOffset();
      }
    };
  }

  public static Function<Token, CharOffset> ToEndCharOffset() {
    return new Function<Token, CharOffset>() {
      @Override
      public CharOffset apply(Token tok) {
        return tok.endCharOffset();
      }
    };
  }

  /**
   * Returns true if and only if the provided {@link Token} is non-empty and every one of its code
   * points is matched by {@code matcher}.
   */
  public static boolean nonEmptyAndAllCodepointsMatch(HasToken x, CodepointMatcher matcher) {
    final String s = x.token().symbol().asString();
    return !s.isEmpty() && matcher.matchesAllOf(s);
  }

  /**
   * Returns true iff every character in the provided token is defined as being capitalized by the
   * Unicode standard. If the token is empty (how?), returns false. This can handle tokens with
   * characters outside the Unicode BMP properly. The output of thus method is undefined if {@code
   * x}'s string contains Unicode combining characters until text-group/jserif#28 is resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static boolean isAllCaps(HasToken x) {
    return nonEmptyAndAllCodepointsMatch(x, CodepointMatcher.uppercase());
  }

  /**
   * Returns true iff every character in the provided token is defined as being a digit by the
   * Unicode standard. If the token is empty (how?), returns false. This can handle tokens with
   * characters outside the Unicode BMP properly. The output of thus method is undefined if {@code
   * x}'s string contains Unicode combining characters until text-group/jserif#28 is resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static boolean isCapitalized(HasToken x) {
    final String s = x.token().symbol().asString();
    return !s.isEmpty() && CodepointMatcher.uppercase().matches(s.codePointAt(0));
  }

  /**
   * Returns true iff every character in the provided token is defined as being a digit by the
   * Unicode standard. If the token is empty (how?), returns false. This can handle tokens with
   * characters outside the Unicode BMP properly. The output of thus method is undefined if {@code
   * x}'s string contains Unicode combining characters until text-group/jserif#28 is resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static boolean isAllDigits(HasToken x) {
    return nonEmptyAndAllCodepointsMatch(x, CodepointMatcher.digit());
  }

  /**
   * Returns true iff every character in the provided token passes either {@link
   * Character#isAlphabetic(int)}. If the token is empty (how?), returns false. This can handle
   * tokens with characters outside the Unicode BMP properly. The output of thus method is undefined
   * if {@code x}'s string contains Unicode combining characters until text-group/jserif#28 is
   * resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static boolean isAlphabetical(HasToken x) {
    return nonEmptyAndAllCodepointsMatch(x, CodepointMatcher.alphabetic());
  }

  /**
   * Returns true iff every character in the provided token passes either {@link
   * Character#isAlphabetic(int)} or {@link Character#isDigit(char)}. If the token is empty (how?),
   * returns false. This can handle tokens with characters outside the Unicode BMP properly. The
   * output of thus method is undefined if {@code x}'s string contains Unicode combining characters
   * until text-group/jserif#28 is resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static boolean isAlphanumeric(HasToken x) {
    return nonEmptyAndAllCodepointsMatch(x, CodepointMatcher.alphanumeric());
  }

  /**
   * Returns the number of Unicode codepoints in the provided {@link Token}'s {@link
   * Token#symbol()}. This can handle tokens with characters outside the Unicode BMP properly.
   * Unpaired surrogates are counted as a single codepoint.
   */
  public static int numCodepoints(HasToken x) {
    final String s = x.token().symbol().asString();
    return Character.codePointCount(s, 0, s.length());
  }


  /**
   * Returns true if the token is non-empty and every code point is matched by {@link
   * CodepointMatcher#punctuation()}.
   */
  public static boolean isAllPuncutation(HasToken x) {
    return nonEmptyAndAllCodepointsMatch(x, CodepointMatcher.punctuation());
  }

  /**
   * Returns true iff the provided {@link Token}'s {@link Token#symbol()} consists of a single
   * codepoint found in the Unicode general category {@link Character#CURRENCY_SYMBOL}.
   */
  public static boolean isCurrencySymbol(HasToken x) {
    return numCodepoints(x) == 1
        && CodepointMatcher.currencySymbols().matchesAllOf(x.token().symbol().asString());
  }

  private static final Pattern URL_PATTERN = Pattern.compile("^(http|www\\.|mailto)");

  public static boolean isURL(HasToken x) {
    final String s = x.token().symbol().asString();
    return URL_PATTERN.matcher(s).find();
  }

  /**
   * Returns the first {@code logicalCharsRequested} Unicode code points of {@code x}'s string. If
   * not enough are available, this returns as many as possible. The output of thus method is
   * undefined if {@code x}'s string contains Unicode combining characters until
   * text-group/jserif#28 is resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static String prefix(HasToken x, int logicalCharsRequested) {
    checkArgument(logicalCharsRequested >= 0);
    final String s = x.token().symbol().asString();
    if (!s.isEmpty()) {
      final StringBuilder ret = new StringBuilder();
      // why this loop? Java stores strings as UTF-16, so logical characters may be one or two
      // chars. We need to be careful in case we get e.g. Asian language input
      for (int charOffset = 0, logicalCharacters = 0;
           charOffset < s.length() && logicalCharacters < logicalCharsRequested;
           ++logicalCharacters) {
        final int codePoint = s.codePointAt(charOffset);
        ret.appendCodePoint(codePoint);
        charOffset += Character.charCount(codePoint);
      }
      return ret.toString();
    } else {
      return "";
    }
  }

  /**
   * Returns the last {@code logicalCharsRequested} Unicode code points of {@code x}'s string. If
   * not enough are available, this returns as many as possible.
   *
   * The output of thus method is undefined if {@code x}'s string contains Unicode combining
   * characters until text-group/jserif#28 is resolved.
   *
   * The output is undefined if the input string contains unpaired surrogates, which it ought not.
   */
  public static String suffix(HasToken x, int logicalCharsRequested) {
    checkArgument(logicalCharsRequested >= 0);
    final String s = x.token().symbol().asString();
    if (!s.isEmpty()) {
      final StringBuilder ret = new StringBuilder();
      // why this loop? Java stores strings as UTF-16, so logical characters may be one or two
      // chars. We need to be careful in case we get e.g. Asian language input
      for (int charOffset = s.length(), logicalCharacters = 0;
           charOffset > 0 && logicalCharacters < logicalCharsRequested;
           ++logicalCharacters) {
        final int codePoint = s.codePointBefore(charOffset);
        ret.appendCodePoint(codePoint);
        charOffset -= Character.charCount(codePoint);
      }
      // StringBuilder's reverse is unicode aware.
      return ret.reverse().toString();
    } else {
      return "";
    }
  }

  /**
   * Creates a single token whose content is exactly the provided string. This is for testing
   * purposes only.
   */
  public static Token asLoneToken(String s) {
    return asLoneToken(StringUtils.unicodeFriendly(s));
  }

  /**
   * Creates a single token whose content is exactly the provided string. This is for testing
   * purposes only.
   */
  public static Token asLoneToken(UnicodeFriendlyString s) {
    final TokenSequence ts = TokenSequence.withOriginalText(0,
        LocatedString.fromReferenceString(s))
        .addToken(Symbol.from(s), OffsetRange.charOffsetRange(0, s.lengthInCodePoints() - 1))
        .build();
    return ts.token(0);
  }

}
