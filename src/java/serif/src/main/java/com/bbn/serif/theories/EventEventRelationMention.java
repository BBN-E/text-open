package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.icewseventmentions.ICEWSEventMention;

import com.bbn.serif.types.Polarity;
import com.google.common.base.Optional;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkNotNull;

@Value.Immutable
@TextGroupImmutable
public abstract class EventEventRelationMention {

  public abstract Symbol relationType();

  public abstract Argument leftEventMention();

  public abstract Argument rightEventMention();

  public abstract Optional<Double> confidence();

  public abstract Optional<String> pattern();

  public abstract Optional<String> model();

  public abstract Optional<Polarity> polarity();

  public abstract Optional<String> triggerText();

  public static class Builder extends ImmutableEventEventRelationMention.Builder {

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

  public static class EventMentionArgument extends Argument {

    public EventMention eventMention() {
      return eventMention;
    }

    private final EventMention eventMention;

    public EventMentionArgument(EventMention eventMention, Symbol role) {
      super(role);
      this.eventMention = checkNotNull(eventMention);
    }
  }

  public static class ICEWSEventMentionArgument extends Argument {

    public ICEWSEventMention icewsEventMention() {
      return icewsEventMention;
    }

    private final ICEWSEventMention icewsEventMention;

    public ICEWSEventMentionArgument(ICEWSEventMention icewsEventMention, Symbol role) {
      super(role);
      this.icewsEventMention = checkNotNull(icewsEventMention);
    }
  }
}

