package com.bbn.serif.driver;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;

import java.io.IOException;

/**
 * Convenience abstract bas class for {@link ProcessingStep}.
 */
@Beta
public abstract class AbstractProcessingStep implements ProcessingStep {
  @Override
  public Function<DocTheory, DocTheory> asFunction() {
    return new Function<DocTheory, DocTheory>() {
      @Override
      public DocTheory apply(final DocTheory input) {
        try {
          return process(input);
        } catch (Throwable t) {
          throw new RuntimeException(t);
        }
      }
    };
  }

  @Override
  public void finish() throws IOException {
    // no logging on completion by default
  }
}
