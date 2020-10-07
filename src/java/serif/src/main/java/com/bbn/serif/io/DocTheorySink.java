package com.bbn.serif.io;

import com.bbn.bue.common.Finishable;
import com.bbn.serif.theories.DocTheory;

import java.io.IOException;

/**
 * Represents a {@code DocTheory} consumer.
 */
public interface DocTheorySink extends Finishable {

  void consume(final DocTheory docTheory) throws IOException;

}
