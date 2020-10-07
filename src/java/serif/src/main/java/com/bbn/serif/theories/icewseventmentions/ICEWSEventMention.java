package com.bbn.serif.theories.icewseventmentions;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.actors.ActorMention;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

@Value.Immutable
@Value.Enclosing
@Functional
@TextGroupImmutable
@JsonSerialize(as = ImmutableICEWSEventMention.class)
@JsonDeserialize(as = ImmutableICEWSEventMention.class)
public abstract class ICEWSEventMention {

  public abstract Symbol code();

  public abstract Symbol tense();

  public abstract Symbol patternId();

  public abstract Optional<ValueMention> timeValueMention();

  public abstract ImmutableList<Proposition> propositions();

  public abstract ImmutableList<ICEWSEventParticipant> eventParticipants();

  public abstract Optional<Symbol> originalEventId();

  @Value.Default
  public boolean isReciprocal() {
    return false;
  }

  @Value.Check
  void check() {
    checkArgument(code().asString().length() >= 2);  // Necessary for topLevelCode()
    if (timeValueMention().isPresent()) {
     checkArgument(timeValueMention().get().isTimexValue());
    }
  }

  public int numEventParticipants() {
    return eventParticipants().size();
  }

  public ICEWSEventParticipant eventParticipant(int idx) {
    return eventParticipants().get(idx);
  }

  public Symbol topLevelCode() {
    return Symbol.from(code().asString().substring(0, 2));
  }

  public Optional<ICEWSEventParticipant> eventParticipantForRole(Symbol role) {
    ICEWSEventParticipant participantForRole = null;
    for (ICEWSEventParticipant participant : eventParticipants()) {
      if (participant.role().toString().equals(role.toString())) {
        participantForRole = participant;
        break;
      }
    }
    return Optional.fromNullable(participantForRole);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("ICEWSEventMention {\n");
    for (ICEWSEventParticipant participant : eventParticipants()) {
      builder.append("\t" + participant.toString() + "\n");
    }
    builder.append("isReciprocal: " + isReciprocal() + "\n");
    if (originalEventId().isPresent()) {
      builder.append("originalEventId: " + originalEventId().get().asString() + "\n");
    }
    builder.append("}");
    return builder.toString();
  }

  public static class Builder extends ImmutableICEWSEventMention.Builder {}

  @Value.Immutable
  @Functional
  @TextGroupImmutable
  @JsonSerialize(as = ImmutableICEWSEventMention.ICEWSEventParticipant.class)
  @JsonDeserialize(as = ImmutableICEWSEventMention.ICEWSEventParticipant.class)
  public static abstract class ICEWSEventParticipant {

    public abstract ActorMention actorMention();

    public abstract Symbol role();

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("ICEWSEventParticipant {\n");
      builder.append("\t" + role() + "\n");
      builder.append("\t" + actorMention().toString() + "\n");
      builder.append("}");
      return builder.toString();
    }

    public static class Builder extends ImmutableICEWSEventMention.ICEWSEventParticipant.Builder {}
  }
}
