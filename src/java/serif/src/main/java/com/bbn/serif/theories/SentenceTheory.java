package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.theories.actors.ActorMentions;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.List;

import static com.bbn.serif.theories.Spannings.toSpanFunction;
import static com.bbn.serif.theories.Spans.ContainedIn;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.compose;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;

/**
 * Represents Serif's analysis of a single sentence.
 *
 * Equality behavior should not be relied on - it is currently identity equality, but we
 * are working on migrating to content-based equality.
 */
@TextGroupImmutable
@Value.Immutable(prehash = true)
@JsonSerialize
@JsonDeserialize
@Functional
public abstract class SentenceTheory implements Spanning, WithSentenceTheory {

  public abstract TokenSequence tokenSequence();
  public abstract ImmutableList<MorphTokenSequence> morphTokenSequences();

  @Value.Default
  public Names names() {
    return Names.absent(tokenSequence());
  }

  public abstract Optional<NestedNames> nestedNames();

  @Value.Default
  public Parse parse() {
    return Parse.absent();
  }

  @Value.Default
  public Mentions mentions() {
    return Mentions.absent();
  }

  /**
   * Those value mentions attached to the sentence theory directly (which is most of them). Value
   * mentions can also occur at the document level, so in many cases you should use {@link
   * #valueMentionsWithDocLevel(DocTheory)} instead.
   */
  @Value.Default
  public ValueMentions valueMentions() {
    return ValueMentions.absent();
  }

  @Value.Default
  public Propositions propositions() {
    return Propositions.absent();
  }

  @Value.Default
  public Dependencies dependencies() {
    return Dependencies.absent();
  }

  @Value.Default
  public RelationMentions relationMentions() {
    return RelationMentions.absent();
  }

  @Value.Default
  public EventMentions eventMentions() {
    return EventMentions.absent();
  }

  @Value.Default
  public ActorMentions actorMentions() {
    return ActorMentions.absent();
  }

  public int sentenceNumber() {
    return sentence().sentenceNumber();
  }

  public abstract Sentence sentence();

  @Deprecated
  public int idx() {
    return sentenceNumber();
  }

  /**
   * @deprecated Prefer {@link #sentenceNumber()}.
   */
  @Deprecated
  public int index() {
    return sentenceNumber();
  }

  /**
   * SERIF, unfortunately, can produce sentences with no tokens. :: sigh ::. This will test if this
   * sentence is one of those. Beware that if it is, calling {@link #span()} will result in a
   * crash.
   */
  public boolean isEmpty() {
    return tokenSequence().isEmpty();
  }

  /**
   * Gets the value mentions directly attached to this sentence as well as those at the document
   * level which are within the bounds of this sentence. In most circumstances this is to be
   * preferred to {@link #valueMentions()}.
   */
  public ImmutableList<ValueMention> valueMentionsWithDocLevel(DocTheory dt) {
    if (tokenSequence().isEmpty()) {
      return ImmutableList.of();
    }
    final TokenSequence.Span thisSentencesSpan = span();
    // a value mention is included if
    return ImmutableList.copyOf(concat(
        // it is attached to this sentence
        valueMentions().asList(),
        // or it is a doc-level VM whose span is contained in this
        // sentence's span
        filter(dt.valueMentions(), compose(ContainedIn(thisSentencesSpan), toSpanFunction()))));
  }

  @Value.Check
  protected void check() {
    // The below check only makes sense if we have an actual token sequence
    if (!tokenSequence().isAbsent()) {
      checkArgument(sentence().sentenceNumber() == tokenSequence().sentenceIndex());
    }
    if (nestedNames().isPresent()) {
      checkArgument(nestedNames().get().parent() == names(),
          "Nested name theory must match name theory");
    }
  }

  @Override
  public TokenSequence.Span span() {
    return tokenSequence().span();
  }

  @Override
  public TokenSpan tokenSpan() {
    return span();
  }

  public Builder modifiedCopyBuilder() {
    return new Builder().from(this);
  }

  public static Builder createForTokenSequence(Sentence sentence, TokenSequence ts) {
    return new Builder().sentence(sentence).tokenSequence(ts);
  }

  @Value.Default
  public boolean hasPOSSequence() {
    return false;
  }

  public static class Builder extends ImmutableSentenceTheory.Builder {

    /**
     * @deprecated Prefer {@link #eventMentions(EventMentions)} (EventMentions)}.
     */
    @Deprecated
    public Builder setEventMentions(final Parse parse, final List<EventMention> eventMentions) {
      return this.eventMentions(new EventMentions.Builder()
          .parse(parse)
          .eventMentions(eventMentions).build());
    }


    /**
     * Sets the name theory of the modified sentence. Setting the name theory will set the nested
     * name theory to absent, since they must be compatible.
     */
    public Builder withNameTheory(final Names names) {
      return this.names(names).nestedNames(Optional.<NestedNames>absent());
    }

  }

  public static final Ordering<SentenceTheory> BySentenceNumber = new Ordering<SentenceTheory>() {

    @Override
    public int compare(final SentenceTheory left, final SentenceTheory right) {
      return Ints.compare(left.sentenceNumber(), right.sentenceNumber());
    }

  };


  /**
   * ************************** If only we could use Java 8 ...... *************************
   */
  public static Predicate<SentenceTheory> isNonEmptyPredicate() {
    return new Predicate<SentenceTheory>() {
      @Override
      public boolean apply(SentenceTheory input) {
        return !input.isEmpty();
      }
    };
  }

  public static Predicate<SentenceTheory> isEmptyPredicate() {
    return new Predicate<SentenceTheory>() {
      @Override
      public boolean apply(SentenceTheory input) {
        return input.isEmpty();
      }
    };
  }

  public static Function<SentenceTheory, Integer> indexFunction() {
    return new Function<SentenceTheory, Integer>() {
      @Override
      public Integer apply(SentenceTheory input) {
        return input.sentenceNumber();
      }
    };
  }

  // we explicitly force identity-based hashCode and equality until we are finished
  // migrating sub-components to Immutables
  @Override
  public boolean equals(Object o) {
    return o == this;
  }

  @Override
  public int hashCode() {
    return System.identityHashCode(this);
  }

}
