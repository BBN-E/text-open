package com.bbn.serif.theories;

import com.google.common.base.Function;

public final class TheoryUtils {
  private TheoryUtils() {
    throw new UnsupportedOperationException();
  }

  public static final Function<HasEventMention, EventMention> eventMentionFunction() {
    return new Function<HasEventMention, EventMention>() {
      @Override
      public EventMention apply(final HasEventMention input) {
        return input.eventMention();
      }
    };
  }
}
