package com.bbn.serif.theories.actors;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;

import com.google.common.base.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ProperNounActorMention implements ActorMention {

  public static final double DEFAULT_ASSOCIATION_SCORE = 0.0;
  public static final double DEFAULT_PATTERN_CONFIDENCE_SCORE = 0.0;
  public static final double DEFAULT_PATTERN_MATCH_SCORE = 0.0;
  public static final double DEFAULT_EDIT_DISTANCE_SCORE = 0.0;
  public static final double DEFAULT_GEO_RESOLUTION_SCORE = 0.0;
  public static final double DEFAULT_IMPORTANCE_SCORE = 0.0;

  private final Symbol actorName;
  private final Symbol sourceNote;
  private final Mention mention;

  private final Optional<Long> actorPatternID;
  private final long actorID;

  private final double associationScore;
  private final double patternConfidenceScore;
  private final double patternMatchScore;
  private final double editDistanceScore;
  private final double geoResolutionScore;
  private final double importanceScore;

  private final GeoResolvedActor geoResolvedActor;

  private final Optional<Symbol> actorDBName;
  private final Optional<Boolean> isAcronym;
  private final Optional<Boolean> requiresContext;

  private ProperNounActorMention(final Optional<Long> actorPatternID, final long actorID,
      final Optional<Symbol> actorName,
      final Symbol sourceNote, final GeoResolvedActor geoResolvedActor,
      final double associationScore,
      final double patternConfidenceScore, final double patternMatchScore, final double
      editDistanceScore, final double geoResolutionScore, final double importanceScore,
      final Mention mention, final Optional<Symbol> actorDBName, final Optional<Boolean> isAcronym,
      final Optional<Boolean> requiresContext) {
    this.actorName = actorName.orNull();
    this.sourceNote = checkNotNull(sourceNote);
    this.mention = checkNotNull(mention);

    this.actorPatternID = actorPatternID;
    this.actorID = actorID;
    this.associationScore = associationScore;
    this.patternConfidenceScore = patternConfidenceScore;
    this.patternMatchScore = patternMatchScore;
    this.editDistanceScore = editDistanceScore;
    this.geoResolutionScore = geoResolutionScore;
    this.importanceScore = importanceScore;

    this.geoResolvedActor = geoResolvedActor;

    this.actorDBName = actorDBName;
    this.isAcronym = isAcronym;
    this.requiresContext = requiresContext;
  }

  public static ProperNounActorMention create(final Optional<Long> actorPatternID, final long actorID,
      final Optional<Symbol> actorName, final Symbol sourceNote, final GeoResolvedActor geoResolvedActor,
      final double associationScore, final double patternConfidenceScore, final double patternMatchScore, final double
      editDistanceScore, final double geoResolutionScore, final double importanceScore,
      final Mention mention, final Optional<Symbol> actorDBName, final Optional<Boolean> isAcronym,
      final Optional<Boolean> requiresContext) {
    return new ProperNounActorMention(actorPatternID, actorID, actorName, sourceNote,
        geoResolvedActor, associationScore, patternConfidenceScore, patternMatchScore,
        editDistanceScore, geoResolutionScore, importanceScore, mention, actorDBName, isAcronym,
        requiresContext);
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

  public Optional<Long> actorPatternID() {
    return actorPatternID;
  }

  public long actorID() {
    return actorID;
  }

  public double associationScore() {
    return associationScore;
  }

  public double patternConfidenceScore() {
    return patternConfidenceScore;
  }

  public double patternMatchScore() {
    return patternMatchScore;
  }

  public double editDistanceScore() {
    return editDistanceScore;
  }

  public double geoResolutionScore() {
    return geoResolutionScore;
  }

  public double importanceScore() {
    return importanceScore;
  }

  public Optional<GeoResolvedActor> geoResolvedActor() {
    return Optional.fromNullable(geoResolvedActor);
  }

  @Override
  public Optional<Symbol> actorDBName(){
    return actorDBName;
  }

  public Optional<Boolean> isAcronym() { return isAcronym; }

  public Optional<Boolean> requiresContext() { return requiresContext; }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    //String sentenceTheoryString = sentenceTheory.toString();
    String mentionString = this.mention.toString();
    String geoResolvedActorString = geoResolvedActor != null ? geoResolvedActor.toString() : "null";

    sb.append(String.valueOf(actorID)).append("[");
    sb.append("actorPatternID=").append(actorPatternID.isPresent()?String.valueOf(actorPatternID.get()):"").append(";");
    sb.append("actorName=").append(
        actorName().isPresent() ? actorName.toString() : "").append(";");
    sb.append("sourceNote=").append(sourceNote.toString()).append(";");
    sb.append("associationScore=").append(String.valueOf(associationScore)).append(";");
    sb.append("patternConfidenceScore=").append(
        String.valueOf(patternConfidenceScore)).append(";");
    sb.append("patternMatchScore=").append(String.valueOf(patternMatchScore)).append(";");
    sb.append("editDistanceScore=").append(String.valueOf(editDistanceScore)).append(";");
    sb.append("geoResolutionScore=").append(String.valueOf(geoResolutionScore)).append(";");

    //sb.append("sentenceTheory=").append(sentenceTheoryString).append(";");
    sb.append("mention=").append(mentionString).append(";");
    sb.append("geoResolvedActor=").append(geoResolvedActorString);
    sb.append("actorDBName=").append(actorDBName.isPresent()?actorDBName.get().asString():"");
    sb.append("]");
    return sb.toString();
  }

}
