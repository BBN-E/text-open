package com.bbn.serif.tokens.constraints;

import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.serif.theories.Token;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint to ensure that a range must be broken into a token.
 */
@Beta
public class MustBeAToken implements TokenizationConstraint {

  private final OffsetGroupRange offsets;

  private MustBeAToken(final OffsetGroupRange offsets) {
    this.offsets = checkNotNull(offsets);
  }

  public static MustBeAToken create(final OffsetGroupRange offsets) {
    return new MustBeAToken(offsets);
  }

  @Override
  public boolean satisfiedBy(final LocatedString sentence, final Iterable<Token> sequence) {
    for (final Token t : sequence) {
      if (t.startCharOffset().equals(offsets.startInclusive().charOffset()) && t.endCharOffset()
          .equals(offsets.endInclusive().charOffset())) {
        return true;
      }
    }
    return false;
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
    final MustBeAToken that = (MustBeAToken) o;
    return Objects.equals(offsets, that.offsets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(offsets);
  }
}
