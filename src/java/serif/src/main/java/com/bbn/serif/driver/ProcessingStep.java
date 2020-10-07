package com.bbn.serif.driver;

import com.bbn.bue.common.Finishable;
import com.bbn.serif.constraints.SerifConstraints;
import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;

import java.io.IOException;

/**
 * Returns a new {@code DocTheory} with results of {@code ProcessingStep} added.
 */
@Beta
public interface ProcessingStep extends Finishable {

  DocTheory process(final DocTheory docTheory) throws IOException;

  DocTheory process(final DocTheory docTheory, final SerifConstraints constraints)
      throws IOException;

  Function<DocTheory, DocTheory> asFunction();
}

