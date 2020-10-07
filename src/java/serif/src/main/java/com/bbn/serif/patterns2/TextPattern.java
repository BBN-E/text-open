package com.bbn.serif.patterns2;


import com.bbn.bue.common.SExpression;
import com.bbn.bue.common.TextGroupImmutable;

import com.google.common.annotations.Beta;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * TextPatterns don’t match over anything on their own; they specify text strings that serve as
 * building blocks for {@link RegexPattern}s. A {@code TextPattern} specifies a span of text
 * consisting of any sequence of strings and wordsets. By default, the sub-components of a
 * {@code TextPattern} are assumed to be joined by spaces and will be matched against the
 * space-joined tokenized text of documents.
 *
 * Note that CSerif limits each string element to 256 characters. This limit is not present in
 * the JSerif implementation.
 *
 * Text patterns may have the following additional constraints specified:
 *
 * <ul>
 *
 *   <li>{@code RAW_TEXT}: Escape any special regular expression characters other than
 *   parentheses in the string.</li>
 *
 *   <li>{@code DONT_ADD_SPACES}: when concatenating the sub-patterns, don't assume there
 *   are token breaks between them.</li>
 *
 * </ul>
 * Examples:
 *
 * <ul>
 *
 * <li><pre>
 *   (text (string DEATH_WORDS "(in|on)?"))
 *   </pre>
 *
 * Used in a regular expression, will match a span of text with a word from the word list
 * {@code DEATH_WORDS} followed possibly by “in” or “on”
 *
 * </li>
 *
 * <li><pre>(text (string ".{0,10} born( in| on)?"))</pre>
 *
 * Used in a regular expression will match up to ten characters, then the word “born” then
 * optionally the word “in” or “on”</li>
 *
 * </ul>
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class TextPattern implements Pattern {
  public abstract String text();

  @Value.Default
  public boolean addSpaces() {
    return true;
  }

  @Value.Default
  public boolean rawText() {
    return false;
  }

  @Value.Check
  protected void check() {
    checkArgument(!text().isEmpty());
  }

  public static class Builder extends ImmutableTextPattern.Builder {}

  public static TextPattern of(String text) {
    return new Builder().text(text).build();
  }

  @Override
  public SExpression toSexpression() {
    // TODO: #336
    throw new UnsupportedOperationException();
  }
}
