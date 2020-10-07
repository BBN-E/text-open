package com.bbn.serif.theories;

import com.bbn.bue.common.observers.Observer;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

public final class Zoning {

  private final ImmutableList<Zone> rootZones;

  public List<Zone> rootZones() {
    return rootZones;
  }

  public static Zoning create(Iterable<Zone> rootZones) {
    return new Zoning(rootZones);
  }

  /**
   * Taking each root zone in order, performs a pre-order traversal applying the specified {@link
   * com.bbn.bue.common.observers.Observer})
   */
  public void observeZones(Observer<Zone> observer) {
    for (final Zone zone : rootZones) {
      visitZone(zone, observer);
    }
  }

  /**
   * Get the {@code Zone} deepest in the hierarchy which entirely contains {@code span}, going by
   * character offsets. If no such {@code Zone} exists, returns {@link
   * com.google.common.base.Optional#absent()}.
   */
  public Optional<Zone> smallestZoneContaining(TokenSequence.Span span) {
    return smallestZoneContaining(span.charOffsetRange());
  }

  /**
   * Get the {@code Zone} deepest in the hierarchy which entirely contains {@code range}, going by
   * character offsets. If no such {@code Zone} exists, returns {@link
   * com.google.common.base.Optional#absent()}.
   */
  public Optional<Zone> smallestZoneContaining(OffsetRange<CharOffset> range) {
    for (final Zone rootZone : rootZones()) {
      final Optional<Zone> smallestContaining = rootZone.smallestZoneContaining(range);
      if (smallestContaining.isPresent()) {
        return smallestContaining;
      }
    }

    return Optional.absent();
  }

  private void visitZone(Zone zone, Observer<Zone> observer) {
    observer.observe(zone);
    for (final Zone kid : zone.children()) {
      visitZone(kid, observer);
    }
  }

  private Zoning(Iterable<Zone> rootZones) {
    this.rootZones = ImmutableList.copyOf(rootZones);
  }

  public ImmutableList<Zone> allZonesContaining(OffsetRange<CharOffset> range) {
    for (final Zone rootZone : rootZones()) {
      if (rootZone.asCharOffsetRange().contains(range)) {
        return rootZone.allZonesContaining(range);
      }
    }
    return ImmutableList.of();
  }
}
