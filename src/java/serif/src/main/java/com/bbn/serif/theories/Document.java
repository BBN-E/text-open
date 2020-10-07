package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.Segment;
import com.bbn.serif.languages.SerifLanguage;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.immutables.value.Value;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;


@JsonSerialize
@JsonDeserialize(as=ImmutableDocument.class)
@TextGroupImmutable
@Value.Immutable
public abstract class Document implements WithDocument {

  public abstract Symbol name();

  /**
   * A set of offsets that specify this {@link Document}'s offset into some other text source. All
   * other offsets in this {@link Document} are relative to these offsets, if present. See {@link
   * com.bbn.serif.io.SerifXMLLoader} class documentation for more information about how these are
   * created.
   */
  public abstract Optional<OffsetGroup> offsetIntoSource();

  public abstract Optional<List<Region>> regions();

  public abstract Optional<Zoning> zoning();

  public abstract Optional<List<Segment>> segments();

  public abstract Optional<Metadata> metadata();

  public abstract Optional<Symbol> sourceType();

  public abstract SerifLanguage language();

  public abstract LocatedString originalText();

  public abstract Optional<LocatedString> dateTimeField();

  public abstract Optional<String> url();

  public abstract Optional<Interval> jodaDocumentTimeInterval();

  /**
   * Gets the single day containing this documents time interval, if possible.
   *
   * If this document has a date interval and that date interval is entirely
   * contained within a single day, an arbitrary instant within that day is
   * returned. Otherwise, {@link Optional#absent()} is returned.
   *
   * @return The single day containing this documents time interval
   */
  public final Optional<DateTime> jodaDocumentDate() {
    if (jodaDocumentTimeInterval().isPresent()) {
      final DateTime start = jodaDocumentTimeInterval().get().getStart();
      final DateTime end = jodaDocumentTimeInterval().get().getEnd();
      if (start.getZone().equals(end.getZone()) &&
          start.getYear() == end.getYear() && start.getMonthOfYear() == end.getMonthOfYear() &&
          start.getDayOfMonth() == end.getDayOfMonth()) {
        return Optional.of(start);
      }
    }
    return Optional.absent();
  }

  public static Builder withNameAndLanguage(Symbol name, SerifLanguage serifLanguage) {
    return new Builder().name(name).language(serifLanguage);
  }

  @Value.Check
  protected void check() {
    checkArgument(!name().asString().isEmpty(), "Empty docIDs not allowed");
    checkArgument(originalText().referenceString().isPresent() &&
        originalText().referenceString().get().equals(originalText().content()));
  }

  public static final class Builder extends ImmutableDocument.Builder {

    /**
     * @deprecated Prefer {@link #regions()}
     */
    @Deprecated
    public Builder withRegions(Iterable<Region> regions) {
      return regions(ImmutableList.copyOf(regions));
    }

    /**
     * @deprecated Prefer {@link #zoning()}
     */
    @Deprecated
    public Builder withZoning(Zoning zoning) {
      return zoning(zoning);
    }

    /**
     * @deprecated Prefer {@link #metadata()}
     */
    @Deprecated
    public Builder withMetadata(Metadata metadata) {
      return metadata(metadata);
    }

    /**
     * @deprecated Prefer {@link #sourceType()}
     */
    @Deprecated
    public Builder withSourceType(Symbol sourceType) {
      return sourceType(sourceType);
    }

    /**
     * @deprecated Prefer {@link #originalText()}
     */
    @Deprecated
    public Builder withOriginalText(LocatedString originalText) {
      return originalText(originalText);
    }

    /**
     * @deprecated Prefer {@link #dateTimeField()}
     */
    @Deprecated
    public Builder withDateTimeField(LocatedString dateTimeField) {
      return dateTimeField(dateTimeField);
    }

    /**
     * @deprecated Prefer {@link #segments()}
     */
    @Deprecated
    public Builder withSegments(List<Segment> segments) {
      return segments(segments);
    }

    /**
     * @deprecated Prefer {@link #url()}
     */
    @Deprecated
    public Builder withURL(String URL) {
      return url(URL);
    }

    /**
     * @deprecated Prefer {@link #jodaDocumentTimeInterval()}
     */
    @Deprecated
    public Builder withJodaDocumentTimeInterval(Interval jodaDocumentTimeInterval){
      return jodaDocumentTimeInterval(jodaDocumentTimeInterval);
    }
  }
}
