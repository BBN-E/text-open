package com.bbn.serif.values.constraints;


import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Beta
public class ExactValueMentionConstraint implements ValueMentionConstraint {

  private final OffsetRange<CharOffset> offsets;
  private final Symbol type;
  @Nullable
  private final Symbol subtype;
  @Nullable
  private final Symbol externalID;

  private ExactValueMentionConstraint(final OffsetRange<CharOffset> offsets, final Symbol type,
      @Nullable final Symbol subtype, @Nullable final Symbol externalID) {
    this.offsets = checkNotNull(offsets);
    this.type = checkNotNull(type);
    this.subtype = subtype;
    this.externalID = externalID;
  }

  public static ExactValueMentionConstraint create(final OffsetRange<CharOffset> offsets,
      final Symbol type, @Nullable final Symbol subtype, @Nullable final Symbol externalID) {
    return new ExactValueMentionConstraint(offsets, type, subtype, externalID);
  }

  @Override
  public OffsetRange<CharOffset> offsets() {
    return offsets;
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }

  public Symbol type() {
    return type;
  }

  public Optional<Symbol> externalID() {
    return Optional.fromNullable(externalID);
  }

  @Override
  public String toString() {
    return "ExactValueMentionConstraint{" +
        "offsets=" + offsets +
        ", type=" + type +
        ", subtype=" + subtype +
        ", externalID=" + externalID +
        '}';
  }
}
