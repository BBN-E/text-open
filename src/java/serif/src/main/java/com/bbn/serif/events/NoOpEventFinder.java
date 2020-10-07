package com.bbn.serif.events;

import com.bbn.serif.events.constraints.EventConstraint;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkArgument;

@Beta
public class NoOpEventFinder implements EventFinder {

  @Inject
  NoOpEventFinder() {

  }

  @Override
  public DocTheory addEvents(final DocTheory input, final Set<EventConstraint> constraints) {
    checkArgument(ImmutableList.copyOf(constraints.iterator()).size() == 0,
        "NoOpEventFinder doesn't know how to handle constraints");
    return input;
  }

  @Override
  public void finish() throws IOException {

  }
}
