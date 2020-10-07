package com.bbn.serif.regions;


import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.regions.constraints.RegionConstraint;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Region;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Makes the entire input text one big region. Does not support constraints.
 */
@Beta
@TextGroupImmutable
@Value.Immutable
public abstract class SingletonRegionFinder implements RegionFinder {

  private final Symbol DEFAULT_REGION = Symbol.from("TEXT");

  @Override
  public DocTheory addRegions(final DocTheory docTheory) {
    return addRegions(docTheory, ImmutableSet.<RegionConstraint>of());
  }

  @Override
  public DocTheory addRegions(final DocTheory docTheory, final Set<RegionConstraint> constraints) {
    checkArgument(constraints.isEmpty());

    //noinspection OptionalGetWithoutIsPresent
    final Region bigRegion = new Region.Builder().tag(DEFAULT_REGION)
        .content(docTheory.document().originalText()).build();

    return docTheory.modifiedCopyBuilder()
        .document(docTheory.document().withRegions(ImmutableList.of(bigRegion)))
        .build();
  }

  @Override
  public void finish() {

  }

  public static class Builder extends ImmutableSingletonRegionFinder.Builder {

  }
}
