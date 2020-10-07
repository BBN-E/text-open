package com.bbn.serif.theories;

import com.bbn.bue.common.strings.offsets.OffsetGroupSpan;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class Metadata {

  private final List<OffsetGroupSpan> spans;

  public List<OffsetGroupSpan> spans() {
    return spans;
  }

  public static Metadata create(Iterable<OffsetGroupSpan> spans) {
    return new Metadata(spans);
  }

  public static Metadata createEmpty() {
    return create(ImmutableSet.<OffsetGroupSpan>of());
  }

  /**
   * Only deprecated as public constructor
   */
  @Deprecated
  public Metadata(Iterable<OffsetGroupSpan> spans) {
    this.spans = ImmutableList.copyOf(checkNotNull(spans));
  }
}
