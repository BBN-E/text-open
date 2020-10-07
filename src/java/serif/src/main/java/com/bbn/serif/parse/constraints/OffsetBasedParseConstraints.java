package com.bbn.serif.parse.constraints;


import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

@Beta
public final class OffsetBasedParseConstraints implements ParseConstraint {

  private final OffsetRange<CharOffset> subtreeSpan;

  private OffsetBasedParseConstraints(final OffsetRange<CharOffset> subtreeSpan) {
    this.subtreeSpan = checkNotNull(subtreeSpan);
  }

  public static OffsetBasedParseConstraints create(final OffsetRange<CharOffset> subtreeSpan) {
    return new OffsetBasedParseConstraints(subtreeSpan);
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }

  public OffsetRange<CharOffset> subtreeSpan() {
    return subtreeSpan;
  }

  @Override
  public String toString() {
    return "OffsetBasedParseConstraints{" +
        "subtreeSpan=" + subtreeSpan +
        '}';
  }

  public static Function<OffsetBasedParseConstraints, OffsetRange<CharOffset>> subtreeFunction() {
    return new Function<OffsetBasedParseConstraints, OffsetRange<CharOffset>>() {
      @Nullable
      @Override
      public OffsetRange<CharOffset> apply(
          @Nullable final OffsetBasedParseConstraints offsetBasedParseConstraints) {
        return checkNotNull(offsetBasedParseConstraints).subtreeSpan();
      }
    };
  }
}
