package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.types.Genericity;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Polarity;
import com.bbn.serif.types.Tense;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This exists to support legacy ACE systems. For most new applications,
 * prefer {@link AbstractDocumentEvent},
 * which better aligns with Adept.
 */
public final class Event implements Iterable<EventMention>, HasExternalID {

  public Symbol type() {
    return type;
  }

  public Genericity genericity() {
    return genericity;
  }

  public Modality modality() {
    return modality;
  }

  public Polarity polarity() {
    return polarity;
  }

  public Tense tense() {
    return tense;
  }

  public List<Argument> arguments() {
    return arguments;
  }

  public List<EventMention> eventMentions() {
    return eventMentions;
  }

  @Override
  public Iterator<EventMention> iterator() {
    return eventMentions.iterator();
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  /**
   * prefer specifying the external_id
   */
  @Deprecated
  public Event(Iterable<Argument> arguments, Iterable<EventMention> mentions,
      Symbol type, Genericity genericity,
      Modality modality, Polarity polarity, Tense tense) {
    this(arguments, mentions, type, genericity, modality, polarity, tense, null);
  }

  public Event(Iterable<Argument> arguments, Iterable<EventMention> mentions,
      Symbol type, Genericity genericity,
      Modality modality, Polarity polarity, Tense tense, @Nullable final Symbol external_id) {
    this.external_id = external_id;
    eventMentions = ImmutableList.copyOf(mentions);
    this.arguments = ImmutableList.copyOf(checkNotNull(arguments));
    this.type = type;
    this.genericity = genericity;
    this.modality = modality;
    this.polarity = polarity;
    this.tense = tense;
  }

  private final List<EventMention> eventMentions;
  private final List<Argument> arguments;
  private final Symbol type;
  private final Genericity genericity;
  private final Modality modality;
  private final Polarity polarity;
  private final Tense tense;
  @Nullable
  private final Symbol external_id;

  /**
   * Returns a Guava Function which gets the type of an event mention. When we someday
   * are able to move to Java 8, this will be deprecated and eventually removed.
   */
  public static Function<Event, Symbol> typeFunction() {
    return new Function<Event, Symbol>() {
      @Override
      public Symbol apply(final Event input) {
        return input.type();
      }
    };
  }

  public static abstract class Argument {

    public Symbol role() {
      return role;
    }

    private final Symbol role;

    public Argument(Symbol role) {
      this.role = checkNotNull(role);
    }
  }

  public static class EntityArgument extends Argument {

    public Entity entity() {
      return entity;
    }

    private final Entity entity;

    public EntityArgument(Entity entity, Symbol role) {
      super(role);
      this.entity = checkNotNull(entity);
    }
  }

  public static class ValueArgument extends Argument {

    public Value value() {
      return value;
    }

    private final Value value;

    public static ValueArgument of(final Value value, final Symbol role) {
      return new ValueArgument(value, role);
    }

    public ValueArgument(Value value, Symbol role) {
      super(role);
      this.value = checkNotNull(value);
    }
  }
}
