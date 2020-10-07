package com.bbn.serif.relations.constraints;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Tense;

import com.google.common.base.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;


public class ExactRelationMentionConstraint implements RelationMentionConstraint {

  private final Tense tense;
  private final Modality modality;
  private final Symbol left;
  @Nullable
  private final Symbol right;
  @Nullable
  private final Symbol timeRole;
  @Nullable
  private final Symbol timeArg;
  private final Symbol type;
  @Nullable
  private final Symbol rawType;
  private final double score;
  @Nullable
  private final Symbol external_id;

  private ExactRelationMentionConstraint(final Tense tense, final Modality modality,
      final Symbol left, @Nullable final Symbol right, final Symbol timeRole, final Symbol timeArg,
      final Symbol type, @Nullable final Symbol rawType, final double score, @Nullable final Symbol external_id) {
    this.tense = checkNotNull(tense);
    this.modality = checkNotNull(modality);
    this.left = checkNotNull(left);
    this.right = right;
    this.timeRole = timeRole;
    this.timeArg = timeArg;
    this.type = checkNotNull(type);
    this.rawType = rawType;
    this.score = score;
    this.external_id = external_id;
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }

  public static Builder builder(final Symbol type) {
    return new Builder(type);
  }

  public Symbol type() {
    return type;
  }

  public Optional<Symbol> rawType() {
    return Optional.fromNullable(rawType);
  }

  public Symbol left() {
    return left;
  }

  public Optional<Symbol> right() {
    return Optional.fromNullable(right);
  }

  public Optional<Symbol> timeArg() {
    return Optional.fromNullable(timeArg);
  }

  public Optional<Symbol> timeRole() {
    return Optional.fromNullable(timeRole);
  }

  public Tense tense() {
    return tense;
  }

  public Modality modality() {
    return modality;
  }

  public double score() {
    return score;
  }

  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  @Override
  public String toString() {
    return "ExactRelationMentionConstraint{" +
        "tense=" + tense +
        ", modality=" + modality +
        ", left=" + left +
        ", right=" + right +
        ", timeRole=" + timeRole +
        ", timeArg=" + timeArg +
        ", type=" + type +
        ", rawType=" + rawType +
        ", score=" + score +
        ", external_id=" + external_id +
        '}';
  }

  public static class Builder {

    private Tense tense = Tense.UNSPECIFIED;
    private Modality modality = Modality.OTHER;
    private Symbol left;
    private Symbol right;
    @Nullable
    private Symbol timeRole;
    @Nullable
    private Symbol timeArg;
    private final Symbol type;
    @Nullable
    private Symbol rawType;
    private double score;
    private Symbol external_id;

    private Builder(final Symbol type) {
      this.type = type;
    }

    public Builder withTense(Tense tense) {
      this.tense = tense;
      return this;
    }

    public Builder withModality(Modality modality) {
      this.modality = modality;
      return this;
    }

    public Builder withLeft(Symbol left) {
      this.left = left;
      return this;
    }

    public Builder withRight(Symbol right) {
      this.right = right;
      return this;
    }

    public Builder withTimes(Symbol timeRole, Symbol timeArg) {
      this.timeRole = timeRole;
      this.timeArg = timeArg;
      return this;
    }

    public Builder withRawType(Symbol rawType) {
      this.rawType = rawType;
      return this;
    }

    public Builder withScore(double score) {
      this.score = score;
      return this;
    }

    public Builder withExternalID(Symbol external_id) {
      this.external_id = external_id;
      return this;
    }

    public ExactRelationMentionConstraint build() {
      return new ExactRelationMentionConstraint(tense, modality, left, right, timeRole, timeArg,
          type, rawType, score, external_id);
    }
  }
}
