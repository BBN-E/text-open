package com.bbn.serif.theories.diff;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

/**
 * A strategy for determining the difference, if any, between two Serif objects.
 *
 * @param <T>        The type of item to be diffed.
 * @param <DiffType> The type of difference description returned.
 */
@Beta
interface Differ<T, DiffType extends Difference<T>> {

  /**
   * Returns a description of the difference, if any, between {@code left} and {@code right}. If
   * there are no differences (or no "interesting" differences for an {@code Differ}-specific
   * understanding of "interesting"), {@link Optional#absent()} should be returned.
   * @param left
   * @param right
   * @return
   */
  Optional<DiffType> diff(T left, T right);
}
