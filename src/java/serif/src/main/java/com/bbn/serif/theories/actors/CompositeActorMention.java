package com.bbn.serif.theories.actors;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class CompositeActorMention implements ActorMention {

  private final Symbol actorName;
  private final Symbol sourceNote;
  private final Mention mention;

  private final long pairedAgentID;
  private final Symbol pairedAgentCode;
  private final Optional<Long> pairedAgentPatternID;
  private final Optional<Symbol> pairedAgentName;

  private final Optional<Long> pairedActorID;
  private final Optional<Symbol> pairedActorCode;
  private final Optional<Long> pairedActorPatternID;
  private final Optional<Symbol> pairedActorName;

  private final Optional<Symbol> actorAgentPattern;

  private final Optional<Symbol> actorDBName;

  private CompositeActorMention(final Optional<Symbol> actorName, final Symbol sourceNote,
      final long pairedAgentID,
      final Symbol pairedAgentCode, final Optional<Long> pairedAgentPatternID, final Optional<Symbol> pairedAgentName,
      final Optional<Long> pairedActorID, final Optional<Symbol> pairedActorCode,
      final Optional<Long> pairedActorPatternID, final Optional<Symbol> pairedActorName,
      final Optional<Symbol> actorAgentPattern,
      final Mention mention, final Optional<Symbol> actorDBName) {
    this.actorName = actorName.orNull();
    this.sourceNote = checkNotNull(sourceNote);
    this.mention = checkNotNull(mention);

    this.pairedAgentID = pairedAgentID;
    this.pairedAgentCode = checkNotNull(pairedAgentCode);
    this.pairedAgentPatternID = pairedAgentPatternID;
    this.pairedAgentName = pairedAgentName;

    this.pairedActorID = pairedActorID;
    this.pairedActorCode = pairedActorCode;
    this.pairedActorPatternID = pairedActorPatternID;
    this.pairedActorName = pairedActorName;

    this.actorAgentPattern = actorAgentPattern;

    this.actorDBName = actorDBName;
  }

  public static CompositeActorMention create(final Optional<Symbol> actorName, final Symbol sourceNote,
      final long pairedAgentID,
      final Symbol pairedAgentCode, final Optional<Long> pairedAgentPatternID, final Optional<Symbol> pairedAgentName,
      final Optional<Long> pairedActorID, final Optional<Symbol> pairedActorCode,
      final Optional<Long> pairedActorPatternID, final Optional<Symbol> pairedActorName,
      final Optional<Symbol> actorAgentPattern,
      final Mention mention, final Optional<Symbol> actorDBName) {
    return new CompositeActorMention(actorName, sourceNote, pairedAgentID, pairedAgentCode,
        pairedAgentPatternID, pairedAgentName, pairedActorID, pairedActorCode, pairedActorPatternID, pairedActorName,
        actorAgentPattern, mention, actorDBName);
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

  public long pairedAgentID() {
    return pairedAgentID;
  }

  public Symbol pairedAgentCode() {
    return pairedAgentCode;
  }

  public Optional<Long> pairedAgentPatternID() {
    return pairedAgentPatternID;
  }

  public Optional<Symbol> pairedAgentName(){
	  return pairedAgentName;
  }

  public Optional<Long> pairedActorID() {
    return pairedActorID;
  }

  public Optional<Symbol> pairedActorCode() {
    return pairedActorCode;
  }

  public Optional<Long> pairedActorPatternID() {
    return pairedActorPatternID;
  }

  public Optional<Symbol> pairedActorName(){
	  return pairedActorName;
  }

  public Optional<Symbol> actorAgentPattern() {
    return actorAgentPattern;
  }

  @Override
  public Optional<Symbol> actorDBName(){
    return actorDBName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    //String sentenceTheoryString = sentenceTheory.toString();
    sb.append("actorName=").append(actorName().isPresent() ? actorName.asString() : "")
        .append(";");
    sb.append("sourceNote=").append(sourceNote.toString()).append(";");

    sb.append("pairedAgentID=").append(String.valueOf(pairedAgentID)).append(";");
    sb.append("pairedAgentCode=").append(pairedAgentCode.toString()).append(";");
    sb.append("pairedAgentPatternID=").append(pairedAgentPatternID.isPresent()?String.valueOf(pairedAgentPatternID.get()):"").append(";");
    sb.append("pairedAgentName=").append(pairedAgentName.isPresent()?pairedAgentName.get().asString():"").append(";");

    sb.append("pairedActorId=")
        .append(pairedActorID().isPresent() ? String.valueOf(pairedActorID.get()) : "").append(";");
    sb.append("pairedActorCode=")
        .append(pairedActorCode().isPresent() ? pairedActorCode.get().toString() : "").append(";");
    sb.append("pairedActorPatternId=").append(
        pairedActorPatternID().isPresent() ? String.valueOf(pairedActorPatternID.get()) : "")
        .append(";");
    sb.append("pairedActorName=").append(pairedActorName.isPresent()?pairedActorName.get().asString():"").append(";");

    sb.append("actorAgentPattern=")
        .append(actorAgentPattern().isPresent() ? actorAgentPattern.get().toString() : "")
        .append(";");
    sb.append("actorDBName=")
    .append(actorDBName().isPresent() ? actorDBName.get().asString() : "")
    .append(";");

    String mentionString = mention.toString();
    sb.append("mention=").append(mentionString).append(";");
    sb.append("]");
    return sb.toString();
  }

}
