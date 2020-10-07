package com.bbn.serif.theories;

import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Zone {

  private final Symbol type;
  private final LocatedString content;
  private final ImmutableList<Zone> children;
  private final ImmutableMap<Symbol, LocatedString> attributes;
  private final LocatedString author;
  private final LocatedString dateTime;

  public Symbol tag() {
    return type;
  }

  public LocatedString locatedString() {
    return content;
  }

  public ImmutableList<Zone> children() {
    return children;
  }

  public Set<Symbol> attributeSet() {
    return attributes.keySet();
  }

  public Optional<LocatedString> attribute(Symbol key) {
    return Optional.fromNullable(attributes.get(key));
  }

  public Optional<LocatedString> author() {
    return Optional.fromNullable(author);
  }

  public Optional<LocatedString> dateTime() {
    return Optional.fromNullable(dateTime);
  }

  /**
   * Get the {@code Zone} deepest in the hierarchy rooted at this {@code Zone} which entirely
   * contains {@code range}, going by character offsets. If no such {@code Zone} exists, returns
   * {@link com.google.common.base.Optional#absent()}.
   */
  public Optional<Zone> smallestZoneContaining(OffsetRange<CharOffset> range) {
    checkNotNull(range);

    if (asCharOffsetRange().contains(range)) {
      for (final Zone kid : children()) {
        final Optional<Zone> ret = kid.smallestZoneContaining(range);
        if (ret.isPresent()) {
          return ret;
        }
      }
      return Optional.of(this);
    }

    return Optional.absent();
  }


  /**
   * Creates a zone. Author and dateTime are optional and may be null.
   */
  public static Zone create(Symbol type, LocatedString content, Iterable<Zone> children,
      Map<Symbol, LocatedString> attributes,
                              /* remaining are nullable */
      LocatedString author, LocatedString dateTime) {
    return new Zone(type, content, children, attributes, author, dateTime);
  }

  public static Zone create(Symbol type, LocatedString content) {
    return new Zone(type, content, ImmutableList.<Zone>of(),
        ImmutableMap.<Symbol, LocatedString>of(), null, null);
  }

  private Zone(Symbol type, LocatedString content, Iterable<Zone> children,
      Map<Symbol, LocatedString> attributes, /* nullable */ LocatedString author,
                 /* nullable */ LocatedString dateTime) {
    this.type = checkNotNull(type);
    this.content = checkNotNull(content);
    this.children = ImmutableList.copyOf(children);
    this.attributes = ImmutableMap.copyOf(attributes);
    // this are optional and so may be null
    this.author = author;
    this.dateTime = dateTime;
  }

  @Override
  public String toString() {
    MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(this).add("type", type)
        .add("bounds", content.referenceBounds()).add("nChildren", children.size())
        .add("attributes", attributes);

    if (author != null) {
      helper = helper.add("author", author);
    }

    if (dateTime != null) {
      helper = helper.add("datetime", dateTime);
    }

    return helper.toString();
  }

  public static Function<Zone, Symbol> tagFunction() {
    return new Function<Zone, Symbol>() {
      @Override
      public Symbol apply(Zone input) {
        return input.tag();
      }
    };
  }

  public ImmutableList<Zone> allZonesContaining(OffsetRange<CharOffset> range) {
    final ImmutableList.Builder<Zone> ret = ImmutableList.builder();
    allZonesContainingInternal(range, ret);
    return ret.build();
  }

  public OffsetRange<CharOffset> asCharOffsetRange() {
    return content.referenceBounds().asCharOffsetRange();
  }

  private void allZonesContainingInternal(OffsetRange<CharOffset> range,
      ImmutableList.Builder<Zone> containingZoneList) {
    if (asCharOffsetRange().contains(range)) {
      for (final Zone kid : children()) {
        // at most one child will containing it, since children ought to be disjoint
        kid.allZonesContainingInternal(range, containingZoneList);
      }

      containingZoneList.add(this);
    }
  }
}
