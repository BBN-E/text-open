package com.bbn.serif.theories;

import com.bbn.bue.common.strings.offsets.CharOffset;

import com.google.common.base.Function;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility methods for using with {@link TokenSpanning}s.
 */
public final class TokenSpannings {

  private TokenSpannings() {
    throw new UnsupportedOperationException();
  }

  public static Function<TokenSpanning, CharOffset> startCharOffsetFunction() {
    return StartCharOffsetFunction.INSTANCE;
  }

  private enum StartCharOffsetFunction implements Function<TokenSpanning, CharOffset> {
    INSTANCE;

    @Override
    public CharOffset apply(@Nullable final TokenSpanning o) {
      return checkNotNull(o).tokenSpan().startCharOffset();
    }
  }
}
