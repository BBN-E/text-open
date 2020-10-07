package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.EDTOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Optional;

import org.immutables.func.Functional;
import org.immutables.value.Value;

/**
 * Represents some portion of the text of a document which has been given some sort of marking.
 * For example, this might mark regions of the text which should be processed as natural language
 * text as opposed to regions which should be treated as metadata.
 *
 * The semantics of regions are application-specific.
 */
@TextGroupImmutable
@Value.Immutable
@Functional
public abstract class Region {

  public abstract Optional<Symbol> tag();

  public abstract LocatedString content();

  @Value.Default
  public boolean isSpeakerRegion() {
    return false;
  }

  @Value.Default
  public boolean isReceiverRegion() {
    return false;
  }

  public static class Builder extends ImmutableRegion.Builder {

  }

  // Deprecated methods

  /**
   * @deprecated Prefer to access the {@link LocatedString#referenceBounds()} of {@link #content()}.
   */
  @Deprecated
  public final EDTOffset startEDTOffset() {
    return content().referenceBounds().startEdtOffsetInclusive();
  }

  /**
   * @deprecated Prefer to access the {@link LocatedString#referenceBounds()} of {@link #content()}.
   */
  @Deprecated
  public final EDTOffset endEDTOffset() {
    return content().referenceBounds().endEdtOffsetInclusive();
  }

  /**
   * @deprecated Prefer to access the {@link LocatedString#referenceBounds()} of {@link #content()}.
   */
  @Deprecated
  public final CharOffset startCharOffset() {
    return content().referenceBounds().startCharOffsetInclusive();
  }

  /**
   * @deprecated Prefer to access the {@link LocatedString#referenceBounds()} of {@link #content()}.
   */
  @Deprecated
  public final CharOffset endCharOffset() {
    return content().referenceBounds().endCharOffsetInclusive();
  }

  /**
   * @deprecated Prefer the clearer name {@link #content()}.
   */
  @Deprecated
  public final LocatedString string() {
    return content();
  }

  /**
   * @deprecated Prefer to work with the {@link LocatedString#content()} directly.
   */
  @Deprecated
  public OffsetRange<CharOffset> asCharOffsets() {
    return OffsetRange
        .fromInclusiveEndpoints(content().referenceBounds().startCharOffsetInclusive(),
            content().referenceBounds().endCharOffsetInclusive());
  }

  /**
   * @deprecated Prefer to use {@link Builder} directly, which makes it clearer that the tag is
   * optional.
   */
  @Deprecated
  public static Builder fromTagAndContent(Symbol tag, LocatedString content) {
    return new Builder().tag(tag).content(content);
  }
}
