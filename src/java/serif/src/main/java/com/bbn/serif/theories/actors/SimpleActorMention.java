package com.bbn.serif.theories.actors;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class SimpleActorMention implements ActorMention {

  private final Symbol actorName;
  private final Symbol sourceNote;
  private final Mention mention;

  private SimpleActorMention(final Optional<Symbol> actorName, final Symbol sourceNote,
      final Mention mention) {
    this.actorName = actorName.orNull();
    this.sourceNote = checkNotNull(sourceNote);

    this.mention = checkNotNull(mention);
  }

  @Override
  public Optional<Symbol> actorName() {
    return Optional.fromNullable(actorName);
  }

  @Override
  public Symbol sourceNote() {
    return sourceNote;
  }

  @Override
  public Mention mention() {
    return mention;
  }

  @Override
  public Optional<Symbol> actorDBName(){
	  return Optional.absent();
  }

  public static SimpleActorMention create(final Optional<Symbol> actorName, final Symbol sourceNote,
      final Mention mention) {
    return new SimpleActorMention(actorName, sourceNote, mention);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    //String sentenceTheoryString = sentenceTheory.toString();
    sb.append("actorName=").append(actorName().isPresent() ? actorName.toString() : "")
        .append(";");
    sb.append("sourceNote=").append(sourceNote.toString()).append(";");
    String mentionString = mention.toString();
    sb.append("mention=").append(mentionString).append(";");
    sb.append("]");
    return sb.toString();
  }
}
