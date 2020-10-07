package com.bbn.serif.values.constraints;


import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.temporal.Timex2Time;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Beta
public final class Timex2Constraint implements ValueMentionConstraint {

  private final Symbol timexVal;
  @Nullable
  private final Symbol timexAnchorVal;
  // TODO just store a symbol?
  @Nullable
  private final Timex2Time.AnchorDirection anchorDirection;
  @Nullable
  private final Symbol timexSet;
  @Nullable
  private final Symbol timexMode;
  @Nullable
  private final Symbol timexNonSpecific;
  private final Symbol externalID;
  private final OffsetRange<CharOffset> charOffsets;

  private Timex2Constraint(final Symbol timexVal, @Nullable final Symbol timexAnchorVal,
      @Nullable final Timex2Time.AnchorDirection anchorDirection, @Nullable final Symbol timexSet,
      @Nullable final Symbol timexMode, @Nullable final Symbol timexNonSpecific,
      final OffsetRange<CharOffset> charOffsets, final Symbol externalID) {
    this.timexVal = checkNotNull(timexVal);
    this.timexAnchorVal = timexAnchorVal;
    this.anchorDirection = anchorDirection;
    this.timexSet = timexSet;
    this.timexMode = timexMode;
    this.timexNonSpecific = timexNonSpecific;
    this.charOffsets = checkNotNull(charOffsets);
    this.externalID = externalID;
  }

  public static Timex2Constraint create(final Symbol timexVal,
      @Nullable final Symbol timexAnchorVal,
      @Nullable final Timex2Time.AnchorDirection anchorDirection, @Nullable final Symbol timexSet,
      @Nullable final Symbol timexMode, @Nullable final Symbol timexNonSpecific,
      final OffsetRange<CharOffset> charOffsets, final Symbol externalID) {
    return new Timex2Constraint(timexVal, timexAnchorVal, anchorDirection, timexSet, timexMode,
        timexNonSpecific, charOffsets, externalID);
  }

  public Symbol timexVal() {
    return timexVal;
  }

  public Optional<Symbol> anchorVal() {
    return Optional.fromNullable(timexAnchorVal);
  }

  public Optional<Symbol> anchorDirection() {
    if (anchorDirection == null) {
      return Optional.absent();
    }
    return Optional.of(Symbol.from(anchorDirection.name()));
  }

  public Optional<Symbol> timexSet() {
    return Optional.fromNullable(timexSet);
  }

  public Optional<Symbol> timexMode() {
    return Optional.fromNullable(timexMode);
  }

  public Optional<Symbol> timexNonSpecific() {
    return Optional.fromNullable(timexNonSpecific);
  }

  @Override
  public OffsetRange<CharOffset> offsets() {
    return charOffsets;
  }

  public Optional<Symbol> externalID() {
    return Optional.fromNullable(externalID);
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    return true;
  }
}
