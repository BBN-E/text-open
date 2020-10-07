package com.bbn.serif.regions.constraints;


import com.bbn.bue.common.strings.LocatedString;
import com.bbn.serif.theories.Region;

import com.google.common.annotations.Beta;

/**
 * Constraints on regions for a {@code Document}
 */
@Beta
public interface RegionConstraint {

  boolean satisfiedBy(final LocatedString text, final Region region);
}
