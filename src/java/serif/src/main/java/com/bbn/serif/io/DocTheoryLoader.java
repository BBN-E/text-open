package com.bbn.serif.io;

import com.bbn.serif.theories.DocTheory;

import com.google.common.annotations.Beta;
import com.google.common.io.CharSource;

import java.io.IOException;

/**
 * Represents a loader for {@code DocTheory} objects.
 */
@Beta
public interface DocTheoryLoader {

  DocTheory loadFrom(CharSource source) throws IOException;

}
