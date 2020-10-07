package com.bbn.serif.tokens.constraints;


import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.serif.theories.Token;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A class explicitly banning certain offsets from containment within tokens, forcing those to be
 * token breaks.
 */
@Beta
public final class NoTokenMayContain implements TokenizationConstraint {

  private final OffsetGroup offsets;

  private NoTokenMayContain(final OffsetGroup offsets) {
    this.offsets = checkNotNull(offsets);
  }

  public static NoTokenMayContain create(final OffsetGroup offsets) {
    return new NoTokenMayContain(offsets);
  }

  @Override
  public boolean satisfiedBy(final LocatedString sentence, final Iterable<Token> sequence) {
    final OffsetRange<CharOffset> singleTokenRange =
        OffsetRange.charOffsetRange(offsets.charOffset().asInt(), offsets.charOffset().asInt());
    for (final Token t : sequence) {
      if (t.charOffsetRange().contains(singleTokenRange)) {
        return false;
      }
    }
    return true;
  }

  public OffsetGroup offsets() {
    return offsets;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final NoTokenMayContain that = (NoTokenMayContain) o;
    return Objects.equals(offsets, that.offsets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offsets);
  }
}
