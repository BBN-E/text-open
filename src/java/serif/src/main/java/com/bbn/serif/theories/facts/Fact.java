package com.bbn.serif.theories.facts;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.TokenSpan;
import com.bbn.serif.theories.TokenSpanning;
import com.bbn.serif.theories.ValueMention;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class for the Fact element of Serif XML
 *
 * @author msrivast
 */
public final class Fact implements TokenSpanning {

  private final TokenSpan tokenSpan;
  private final Symbol type;
  private final double score;
  private final Optional<Integer> scoreGroup;
  private final List<Argument> arguments;

  @Override
  public TokenSpan tokenSpan() {
    return tokenSpan;
  }

  /**
   * returns the {@link TokenInStence} object having the start-sentence index and start-token index
   * for this fact.
   *
   * @deprecated
   */
  @Deprecated
  public TokenInSentence startOffset() {
    return TokenInSentence.create(tokenSpan.startTokenSequence().sentenceIndex(),
        tokenSpan.startTokenIndexInclusive());
  }

  /**
   * returns the {@link TokenInSentence} object having the end-sentence index and end-token index
   * for this fact.
   *
   * @deprecated
   */
  @Deprecated
  public TokenInSentence endOffset() {
    return TokenInSentence.create(tokenSpan.endTokenSequence().sentenceIndex(),
        tokenSpan.endTokenIndexInclusive());
  }

  /**
   * returns the type for this fact as a {@link Symbol} object
   */
  public Symbol type() {
    return type;
  }

  /**
   * returns the score for this fact object
   */
  public double score() {
    return score;
  }

  /**
   * returns the score-group for this fact object as an {@link Optional} object
   */
  public Optional<Integer> scoreGroup() {
    return scoreGroup;
  }

  /**
   * returns the list of {@link Argument} objects for this fact An {@link Argument} can be one of
   * {@link MentionArgument}, {@link ValueMentionArgument}, {@link TextSpanArgument}. If the fact
   * object has no arguments, an empty list is returned.
   */
  public List<Argument> arguments() {
    return arguments;
  }

  /**
   * returns an {@link Iterable} of {@link Argument} objects for this fact that are temporal
   * arguments If the fact object has no arguments that are temporal, an empty list is returned.
   */
  public Iterable<Argument> temporalArguments() {
    return Iterables.filter(arguments(), IsTemporalArgument);
  }

  /**
   * returns true, if this fact object has multiple arguments that have the same {@link
   * Argument#role} attribute, else false.
   */
  public boolean hasMultipleArgsWithSameRole() {
    final Multiset<Symbol> types = HashMultiset.create();
    for (final Fact.Argument arg : arguments()) {
      types.add(arg.role());
    }
    for (final Entry<Symbol> entry : types.entrySet()) {
      if (entry.getCount() > 1) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a {@link Collection} of {@link Argument} objects contained in this fact with a
   * particular {@link Argument#role}
   *
   * @param targetRole the role to filter arguments on
   * @return Collection of arguments filtered on the <code>targetRole</code>
   */
  public Collection<Argument> argsForRole(final Symbol targetRole) {
    final ImmutableList.Builder<Argument> ret = ImmutableList.builder();
    for (final Argument arg : arguments()) {
      if (arg.role() == targetRole) {
        ret.add(arg);
      }
    }
    return ret.build();
  }

  /**
   * Private Constructor. Use {@link Fact#create(Symbol, List, double, Optional, TokenInSentence,
   * TokenInSentence)} method to create a {@link Fact} object instead.
   */
  private Fact(final Symbol type, final List<Argument> arguments, final double score,
      final Optional<Integer> scoreGroup, final TokenSpan tokenSpan) {
    this.type = checkNotNull(type);
    this.arguments =
        arguments == null ? Collections.<Argument>emptyList() : ImmutableList.copyOf(arguments);
    this.score = score;
    this.scoreGroup = scoreGroup;
    this.tokenSpan = checkNotNull(tokenSpan);
  }

  /**
   * Use this method to create a new {@link Fact} object
   *
   * @param type       The type of the fact
   * @param arguments  list of {@link Argument} objects for this fact where {@link Argument} can be
   *                   one of {@link MentionArgument}, {@link ValueMentionArgument}, {@link
   *                   TextSpanArgument}
   * @param score      score of the fact
   * @param scoreGroup score-group for the fact
   * @return a new {@link Fact} object
   */
  public static Fact create(final Symbol type, final List<Argument> arguments, final double score,
      final Optional<Integer> scoreGroup, final TokenSpan tokenSpan) {
    return new Fact(type, arguments, score, scoreGroup, tokenSpan);
  }

  /**
   * Abstract class to represent a Fact argument
   *
   * @author msrivast
   */
  public static abstract class Argument {

    private final Symbol role;

    /**
     * Returns a new <code>Argument</code> object with the specified role
     *
     * @param role the role for this fact-argument
     */
    public Argument(final Symbol role) {
      this.role = checkNotNull(role);
    }

    /**
     * returns the {@link Argument#role} for this Argument as a {@link Symbol}
     */
    public Symbol role() {
      return role;
    }
  }

  /**
   * @author msrivast
   */
  public static final class MentionArgument extends Argument implements Spanning {

    private MentionArgument(final Symbol role, final Mention mention) {
      super(role);
      this.mention = checkNotNull(mention);
    }

    public static MentionArgument create(final Symbol role, final Mention mention) {
      return new MentionArgument(role, mention);
    }

    public Mention mention() {
      return mention;
    }

    @Override
    public TokenSequence.Span span() {
      return mention.span();
    }

    @Override
    public TokenSpan tokenSpan() {
      return span();
    }

    @Override
    public String toString() {
      return "MentionFactArg[role=" + super.role + ";" + "mention=" + mention + "]";
    }

    private final Mention mention;
  }

  public static final class ValueMentionArgument extends Argument implements Spanning {

    private ValueMentionArgument(final Symbol role, final ValueMention valueMention,
        final boolean isDocDate) {
      super(role);
      checkArgument((isDocDate && valueMention == null) || (!isDocDate && valueMention != null));
      this.valueMention = valueMention;
      this.isDocDate = isDocDate;
    }

    public static ValueMentionArgument create(final Symbol role, final ValueMention valueMention,
        final boolean isDocDate) {
      return new ValueMentionArgument(role, valueMention, isDocDate);
    }

    public Optional<ValueMention> valueMention() {
      return Optional.fromNullable(valueMention);
    }

    public boolean isDocDate() {
      return isDocDate;
    }

    @Override
    public TokenSequence.Span span() {
      return valueMention.span();
    }

    @Override
    public TokenSpan tokenSpan() {
      return span();
    }

    @Override
    public String toString() {
      return "ValueMentionFactArg[role=" + super.role + ";valueMention=" +
          valueMention + ";isDocDate=" + String.valueOf(isDocDate) + "]";
    }

    private final ValueMention valueMention;
    private final boolean isDocDate;
  }

  public static final class TextSpanArgument extends Argument implements TokenSpanning {

    private TextSpanArgument(final Symbol role, final TokenSpan tokenSpan) {
      super(role);
      this.tokenSpan = checkNotNull(tokenSpan);
    }

    public static TextSpanArgument create(final Symbol role, final TokenSpan tokenSpan) {
      return new TextSpanArgument(role, tokenSpan);
    }

    @Override
    public TokenSpan tokenSpan() {
      return tokenSpan;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("role", role()).add("tokenSpan", tokenSpan())
          .toString();
    }

    private final TokenSpan tokenSpan;

    @Deprecated
    public TokenInSentence startOffset() {
      return TokenInSentence.create(tokenSpan().startTokenSequence().sentenceIndex(),
          tokenSpan().startTokenIndexInclusive());
    }

    @Deprecated
    public TokenInSentence endOffset() {
      return TokenInSentence.create(tokenSpan().endTokenSequence().sentenceIndex(),
          tokenSpan().endTokenIndexInclusive());
    }
  }

  public static final class StringArgument extends Argument {

    private StringArgument(final Symbol role, final Symbol string) {
      super(role);
      this.string = checkNotNull(string);
    }

    public static StringArgument create(final Symbol role, final Symbol string) {
      return new StringArgument(role, string);
    }

    public Symbol string() {
      return string;
    }

    @Override
    public String toString() {
      return "StringFactArg[role=" + super.role + ";" + "string=" + string + "]";
    }

    private final Symbol string;
  }

  private static final Predicate<Argument> IsTemporalArgument = new Predicate<Argument>() {
    @Override
    public boolean apply(final Argument arg) {
      if (arg instanceof ValueMentionArgument) {
        final ValueMentionArgument vmArg = (ValueMentionArgument) arg;
        final boolean valueMentionIsTimex =
            vmArg.valueMention().isPresent() && vmArg.valueMention().get().isTimexValue();
        return valueMentionIsTimex || vmArg.isDocDate();
      } else {
        return false;
      }
    }
  };

  @Deprecated
  public static final class TokenInSentence {

    private final int sentenceIndex;
    private final int tokenIndex;

    private TokenInSentence(int sentenceIndex, int tokenIndex) {
      checkArgument(sentenceIndex >= 0);
      checkArgument(tokenIndex >= 0);

      this.sentenceIndex = sentenceIndex;
      this.tokenIndex = tokenIndex;
    }

    public static TokenInSentence create(int sentenceIndex, int tokenIndex) {
      return new TokenInSentence(sentenceIndex, tokenIndex);
    }

    public int sentenceIndex() {
      return sentenceIndex;
    }

    public int tokenIndex() {
      return tokenIndex;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      TokenInSentence that = (TokenInSentence) o;

      if (sentenceIndex != that.sentenceIndex) {
        return false;
      }
      if (tokenIndex != that.tokenIndex) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = sentenceIndex;
      result = 31 * result + tokenIndex;
      return result;
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append("Fact:").append(type).append("[");
    sb.append("score=").append(String.valueOf(score)).append(";");
    if (scoreGroup.isPresent()) {
      sb.append("scoreGroup=").append(String.valueOf(scoreGroup)).append(";");
    }
    sb.append(";args=").append(StringUtils.SemicolonJoiner.join(arguments()));
    sb.append("]");
    return sb.toString();
  }


}
