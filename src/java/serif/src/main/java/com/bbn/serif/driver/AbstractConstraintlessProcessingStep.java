package com.bbn.serif.driver;

import com.bbn.serif.constraints.SerifConstraints;
import com.bbn.serif.theories.DocTheory;

import java.io.IOException;

/**
 * A {@link com.bbn.serif.driver.ProcessingStep} which doesn't allow constraints.
 */
public abstract class AbstractConstraintlessProcessingStep extends AbstractProcessingStep {
  @Override
  public final DocTheory process(final DocTheory docTheory, final SerifConstraints constraints)
      throws IOException {
    // an exception should be thrown here if there are constraints, but SerifConstraints
    // lacks an "isEmpty".  This is text-group/jserif#241
    return process(docTheory);
  }

  @Override
  public void finish() throws IOException {
    // by default no logging is done on completion
  }
}
