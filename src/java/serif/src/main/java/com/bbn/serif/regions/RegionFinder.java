package com.bbn.serif.regions;


import com.bbn.bue.common.Finishable;
import com.bbn.serif.regions.constraints.RegionConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;

import java.util.Set;

/**
 * Divides a document into regions.  Only text within regions should be processed as human
 * language.
 *
 * Each {@code RegionFinder} is responsible for verifying that the input constraints are obeyed or it
 * is acceptable to ignore them.
 */
@Beta
public interface RegionFinder extends Finishable {

  DocTheory addRegions(final DocTheory docTheory);

  DocTheory addRegions(final DocTheory docTheory, final Set<RegionConstraint> constraints);
}
