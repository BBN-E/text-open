package com.bbn.serif.theories.actors;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Entity;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ActorEntity implements Iterable<ActorMention> {

  private final Optional<Long> actorID;
  private final List<ActorMention> actorMentions;
  private final Entity entity;
  private final double confidence;
  private final Symbol sourceNote;
  private final Symbol actorName;
  private final Optional<Symbol> actorDBName; 

  private ActorEntity(Optional<Long> actorID, Symbol actorName,
      Iterable<ActorMention> mentions,
      Entity entity, double confidence, Symbol sourceNote, Optional<Symbol> actorDBName) {
    this.actorID = actorID;
    this.actorName = checkNotNull(actorName);
    this.actorMentions = ImmutableList.copyOf(mentions);
    this.entity = checkNotNull(entity);
    this.confidence = confidence;
    this.sourceNote = sourceNote;
    this.actorDBName = actorDBName;
  }

  public static ActorEntity createWithMentions(Optional<Long> actorID, Symbol actorName,
      Iterable<ActorMention> mentions, Entity entity, double confidence, Symbol sourceNote,
      Optional<Symbol> actorDBName) {
    return new ActorEntity(actorID, actorName, mentions, entity, confidence,
        sourceNote, actorDBName);
  }

  public static ActorEntity createWithoutMentions(Optional<Long> actorID, Symbol actorName,
      Entity entity, double confidence, Symbol sourceNote, Optional<Symbol> actorDBName) {
    return new ActorEntity(actorID, actorName, ImmutableList.<ActorMention>of(), entity,
        confidence, sourceNote, actorDBName);
  }

  public Optional<Long> actorID() {
    return actorID;
  }

  public List<ActorMention> actorMentions() {
    return actorMentions;
  }

  public double confidence() {
    return confidence;
  }

  public Entity entity() {
    return entity;
  }

  public Optional<Symbol> sourceNote() {
    return Optional.fromNullable(sourceNote);
  }

  public Symbol actorName() {
    return actorName;
  }
  
  public Optional<Symbol> actorDBName() {
	    return actorDBName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();

    sb.append(actorID.isPresent() ? String.valueOf(actorID.get()) : "null").append("[");
    sb.append("actorDBName=").append(actorDBName.isPresent() ? actorDBName.get().asString():"null").append(";");
    sb.append("actorName=").append(actorName.toString()).append(";");
    sb.append("sourceNote=").append(sourceNote == null ? "" : sourceNote.toString()).append(";");
    sb.append("confidence=").append(String.valueOf(confidence)).append(";");
    sb.append("entity=").append(entity.toString()).append(";");
    sb.append("actorMentions=[").append(StringUtils.SemicolonJoiner.join(actorMentions));
    sb.append("]").append("]");
    return sb.toString();
  }

  @Override
  public Iterator<ActorMention> iterator() {
    return actorMentions.iterator();
  }
}
