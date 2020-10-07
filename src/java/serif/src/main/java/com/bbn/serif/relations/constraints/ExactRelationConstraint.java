package com.bbn.serif.relations.constraints;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Tense;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class ExactRelationConstraint implements RelationConstraint {

  // ids
  private final Symbol left;
  // LDC relations can be between an entity and a filler or two entities
  @Nullable
  private final Symbol right;
  @Nullable
  private final Symbol externalID;
  private final ImmutableSet<Symbol> relationMentionIDs;

  private final Tense tense;
  private final Symbol type;
  private final Modality modality;
  private final Float confidence;


  private ExactRelationConstraint(final Symbol left, @Nullable final Symbol right,
      @Nullable final Symbol externalID,
      final Iterable<Symbol> relationMentionIDs, final Tense tense, final Symbol type,
      final Modality modality, final Float confidence) {
    this.left = checkNotNull(left);
    this.right = right;
    this.externalID = externalID;
    this.relationMentionIDs = ImmutableSet.copyOf(relationMentionIDs);
    this.tense = checkNotNull(tense);
    this.type = checkNotNull(type);
    this.modality = checkNotNull(modality);
    this.confidence = checkNotNull(confidence);
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    return true;
  }

  public static Builder builder(final Symbol type) {
    return new Builder(type);
  }

  public Float confidence() {
    return confidence;
  }

  public Symbol externalID() {
    return externalID;
  }

  public Symbol type() {
    return type;
  }

  public Tense tense() {
    return tense;
  }

  public Modality modality() {
    return modality;
  }

  public Symbol right() {
    return right;
  }

  public Symbol left() {
    return left;
  }

  public ImmutableSet<Symbol> relationMentionIDs() {
    return relationMentionIDs;
  }

  public static class Builder {

    private Symbol left;
    private Symbol right;
    private Symbol externalID;
    private Tense tense = Tense.UNSPECIFIED;
    private final Symbol type;
    private Modality modality = Modality.ASSERTED;
    private Float confidence;
    private final Set<Symbol> relationMentionIDs = Sets.newHashSet();

    private Builder(final Symbol type) {
      this.type = type;
    }

    public Builder withLeft(Symbol left) {
      this.left = left;
      return this;
    }

    public Builder withRight(Symbol right) {
      this.right = right;
      return this;
    }

    public Builder withExternalID(Symbol externalID) {
      this.externalID = externalID;
      return this;
    }

    public Builder withTense(Tense tense) {
      this.tense = tense;
      return this;
    }


    public Builder withModality(Modality modality) {
      this.modality = modality;
      return this;
    }

    public Builder withConfidence(Float confidence) {
      this.confidence = confidence;
      return this;
    }

    public ExactRelationConstraint build() {
      ExactRelationConstraint exactRelationConstraint =
          new ExactRelationConstraint(left, right, externalID, relationMentionIDs, tense, type,
              modality, confidence);
      return exactRelationConstraint;
    }

    public Builder withRelationMentions(
        final ImmutableSet<Symbol> relationMentionIds) {
      this.relationMentionIDs.addAll(relationMentionIds);
      return this;
    }
  }
}
