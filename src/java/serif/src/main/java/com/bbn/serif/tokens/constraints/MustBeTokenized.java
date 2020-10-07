package com.bbn.serif.tokens.constraints;


import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.serif.theories.Token;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Ensures that a particular {@link OffsetGroupRange is broken into one or more tokens}
 */
@Beta
public final class MustBeTokenized implements TokenizationConstraint {

  private final OffsetGroupRange offsets;

  private MustBeTokenized(final OffsetGroupRange offsets) {
    this.offsets = checkNotNull(offsets);
  }

  public static MustBeTokenized create(final OffsetGroupRange offsets) {
    return new MustBeTokenized(offsets);
  }

  @Override
  public boolean satisfiedBy(final LocatedString sentence, final Iterable<Token> sequence) {
    boolean foundStart = false;
    boolean foundEnd = false;
    for (final Token t : sequence) {
      if (t.startCharOffset().equals(offsets.asCharOffsetRange().startInclusive()) || t
          .endCharOffset().equals(offsets.asCharOffsetRange().startInclusive())) {
        foundStart = true;
      }
      if (t.startCharOffset().equals(offsets.asCharOffsetRange().endInclusive()) || t
          .endCharOffset().equals(offsets.asCharOffsetRange().endInclusive())) {
        foundEnd = true;
      }
      if(foundEnd && foundStart) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "MustBeTokenized{" +
        "offsets=" + offsets +
        '}';
  }

  public OffsetGroupRange offsets() {
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
    final MustBeTokenized that = (MustBeTokenized) o;
    return Objects.equals(offsets, that.offsets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offsets);
  }
}
