package com.bbn.serif.constraints;

import com.bbn.serif.regions.RegionFinder;
import com.bbn.serif.regions.SingletonRegionFinder;

import com.google.common.annotations.Beta;
import com.google.inject.AbstractModule;

/**
 * Created by rgabbard on 3/18/16.
 */
@Beta
public final class SingletonRegionFinderM extends AbstractModule {

  @Override
  protected void configure() {
    bind(RegionFinder.class).to(SingletonRegionFinder.class);
  }
}
