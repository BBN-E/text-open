package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@JsonSerialize
@JsonDeserialize
@TextGroupImmutable
@Value.Immutable
@Functional
public abstract class EventMentions
    implements Iterable<EventMention>, PotentiallyAbsentSerifTheory, WithEventMentions {

  /**
   * The parse of the sentence the {@link EventMentions} are associated with.
   * This will always be present unless {@link #isAbsent()} is true.
   */
  public abstract Optional<Parse> parse();
  public abstract ImmutableSet<EventMention> eventMentions();

  @Override
  @org.immutables.value.Value.Default
  public boolean isAbsent() {
    return false;
  }

  public static EventMentions absent() {
    return new Builder()
        .parse(Parse.absent())
        .isAbsent(true).build();
  }

  public static EventMentions create(Parse parse, Iterable<EventMention> eventMentions) {
    return new Builder().parse(parse).eventMentions(eventMentions).build();
  }

  public final int size() {
    return numEventMentions();
  }

  public final EventMention get(int idx) {
    return eventMention(idx);
  }

  public final int numEventMentions() {
    return eventMentions().size();
  }

  public final EventMention eventMention(int idx) {
    return asList().get(idx);
  }

  @Override
  public final Iterator<EventMention> iterator() {
    return eventMentions().iterator();
  }

  public final List<EventMention> asList() {
    return eventMentions().asList();
  }

  @Value.Check
  protected void check() {
    checkArgument(!isAbsent() || eventMentions().isEmpty(), "Absent event mentions must be empty");
  }

  public static final Function<SentenceTheory,EventMentions> eventMentionsFunction = new Function<SentenceTheory, EventMentions>() {
    @Override
    public EventMentions apply(final SentenceTheory input) {
      return input.eventMentions();
    }
  };

  public static Function<DocTheory, Integer> eventMentionsCountFunction() {
    return EventMentionsCount.INSTANCE;
  }

  enum EventMentionsCount implements Function<DocTheory, Integer> {
    INSTANCE;
    @Override
    public Integer apply(final DocTheory input) {
      int counter = 0;
      for(SentenceTheory sentenceTheory : input.nonEmptySentenceTheories()) {
        counter += sentenceTheory.eventMentions().numEventMentions();
      }
      return counter;
    }
  }

  public static class Builder extends ImmutableEventMentions.Builder {

  }
}
