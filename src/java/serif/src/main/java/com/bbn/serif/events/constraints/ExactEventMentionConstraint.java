package com.bbn.serif.events.constraints;

import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.types.GainLoss;
import com.bbn.serif.types.Genericity;
import com.bbn.serif.types.Indicator;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Polarity;
import com.bbn.serif.types.Tense;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;


public final class ExactEventMentionConstraint implements EventMentionConstraint {

  public final Symbol GENERIC = Symbol.from("generic");
  public final Symbol OTHER = Symbol.from("other");
  public final Symbol ACTUAL = Symbol.from("actual");

  private final Symbol eventType;
  //  @Nullable
//  private final Symbol patternID;
  private final Modality modality;
  private final double modalityScore;
  private final Polarity polarity;
  private final Genericity genericity;
  private final double genericityScore;
  private final Tense tense;

  @Nullable
  private final Proposition anchorProp;

  // for synnode
  private final int triggerStart;
  private final int triggerEnd;

  private final ImmutableSet<Symbol> mentionIDs;
  private final ImmutableSet<Symbol> valueMentionIDs;
  private final ImmutableSet<OffsetRange<CharOffset>> spanArguments;
  private final ImmutableMap<Symbol, Symbol> idToRole;

  //  @Nullable
//  private final Indicator indicator;
//  @Nullable
//  private final GainLoss gainLoss;
  @Nullable
  private final Symbol external_id;

  private ExactEventMentionConstraint(final Symbol eventType, final Symbol patternID,
      final double modalityScore, final Polarity polarity, final Genericity genericity,
      final double genericityScore, final Tense tense, final Modality modality,
      final Proposition anchorProp,
      final int triggerStart, final int triggerEnd, final Set<Symbol> mentionIDs,
      final Set<Symbol> valueMentionIDs, final Set<OffsetRange<CharOffset>> spanArguments,
      final Indicator indicator, final GainLoss gainLoss,
      final Map<Symbol, Symbol> idToRole, @Nullable final Symbol external_id) {
    this.modality = checkNotNull(modality);
    this.idToRole = ImmutableMap.copyOf(idToRole);
    this.eventType = checkNotNull(eventType);
//    this.patternID = patternID;
    this.modalityScore = modalityScore;
    this.polarity = checkNotNull(polarity);
    this.genericity = checkNotNull(genericity);
    this.genericityScore = genericityScore;
    this.tense = checkNotNull(tense);
    this.anchorProp = anchorProp;
    this.triggerStart = triggerStart;
    this.triggerEnd = triggerEnd;
    this.mentionIDs = ImmutableSet.copyOf(mentionIDs);
    this.valueMentionIDs = ImmutableSet.copyOf(valueMentionIDs);
    this.spanArguments = ImmutableSet.copyOf(spanArguments);
//    this.indicator = indicator;
//    this.gainLoss = gainLoss;
    this.external_id = external_id;
  }


  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }

  public Symbol eventType() {
    return eventType;
  }

  public static Builder builder(final Symbol eventType, final int triggerStart,
      final int triggerEnd) {
    return new Builder(eventType, triggerStart, triggerEnd);
  }

  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  public OffsetRange<CharOffset> triggerOffsets() {
    return OffsetRange.charOffsetRange(triggerStart, triggerEnd);
  }

  public Proposition anchorProp() {
    return anchorProp;
  }

  public Genericity genericity() {
    return genericity;
  }

  public double genericityScore() {
    return genericityScore;
  }

  public Modality modality() {
    return modality;
  }

  public double modalityScore() {
    return modalityScore;
  }

  public Polarity polarity() {
    return polarity;
  }

  public Tense tense() {
    return tense;
  }

  public ImmutableSet<Symbol> mentionIDs() {
    return mentionIDs;
  }

  public ImmutableSet<Symbol> valueMentionIDs() {
    return valueMentionIDs;
  }

  public ImmutableSet<OffsetRange<CharOffset>> spanArguments() {
    return spanArguments;
  }

  public ImmutableMap<Symbol, Symbol> idToRole() {
    return idToRole;
  }

  public static class Builder {

    private final Symbol eventType;
    //    @Nullable
//    private Symbol patternID;
    private Modality modality = Modality.ASSERTED;
    private double modalityScore = 1.0;
    private Polarity polarity = Polarity.POSITIVE;
    private Genericity genericity;
    private double genericityScore;
    private Tense tense = Tense.UNSPECIFIED;

    @Nullable
    private Proposition anchorProp;

    // for synnode
    private final int triggerStart;
    private final int triggerEnd;

    private final Set<Symbol> mentionIDs = Sets.newHashSet();
    private final Set<Symbol> valueMentionIDs = Sets.newHashSet();
    private final Set<OffsetRange<CharOffset>> spanArguments = Sets.newHashSet();
    private final Map<Symbol, Symbol> idToRole = Maps.newHashMap();
    //
//    @Nullable
//    private Indicator indicator = null;
//    @Nullable
//    private GainLoss gainLoss = null;
    @Nullable
    private Symbol external_id = null;

    private Builder(final Symbol eventType,
        final int triggerStart, final int triggerEnd) {
      this.eventType = eventType;
      this.triggerStart = triggerStart;
      this.triggerEnd = triggerEnd;
    }

    public Builder addMention(final Symbol mentionID, final Symbol role) {
      mentionIDs.add(mentionID);
      idToRole.put(mentionID, role);
      return this;
    }

    public Builder addValueMention(final Symbol valueMentionID, final Symbol role) {
      valueMentionIDs.add(valueMentionID);
      idToRole.put(valueMentionID, role);
      return this;
    }

    public Builder setModality(final Modality modality, final double modalityScore) {
      this.modality = checkNotNull(modality);
      this.modalityScore = modalityScore;
      return this;
    }

//    public Builder setPatternID(final Symbol patternID) {
//      this.patternID = patternID;
//      return this;
//    }

    public Builder setPolarity(final Polarity polarity) {
      this.polarity = polarity;
      return this;
    }

    public Builder setGenericity(final Genericity genericity, final double genericityScore) {
      this.genericityScore = genericityScore;
      this.genericity = genericity;
      return this;
    }


    public Builder setTense(final Tense tense) {
      this.tense = tense;
      return this;
    }

    public Builder setAnchorProp(final Proposition anchorProp) {
      this.anchorProp = anchorProp;
      return this;
    }

//    public Builder setIndicator(final Indicator indicator) {
//      this.indicator = indicator;
//      return this;
//    }
//
//    public Builder setGainLoss(final GainLoss gainLoss) {
//      this.gainLoss = gainLoss;
//      return this;
//    }

    public Builder setExternal_id(final Symbol external_id) {
      this.external_id = external_id;
      return this;
    }

    public ExactEventMentionConstraint build() {
      return new ExactEventMentionConstraint(eventType, null, modalityScore, polarity,
          genericity, genericityScore, tense, modality, anchorProp, triggerStart, triggerEnd,
          mentionIDs,
          valueMentionIDs, spanArguments, null, null, idToRole, external_id);
    }
  }
}
