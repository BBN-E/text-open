package com.bbn.serif.theories.diff;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

/**
 * Represents the difference between two objects.
 */
@Beta
interface Difference<T> {
  Optional<T> left();
  Optional<T> right();

  void writeTextReport(final StringBuilder sb, final int indentSpaces);
}
