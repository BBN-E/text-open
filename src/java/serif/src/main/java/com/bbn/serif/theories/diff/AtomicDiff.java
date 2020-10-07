package com.bbn.serif.theories.diff;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

/**
 * Represents the differences between two things without going into details about their internal
 * structure.
 */
@Beta
final class AtomicDiff<T> implements Difference<T> {
  private final @Nullable T left;
  private final @Nullable T right;

  private AtomicDiff(@Nullable final T left, @Nullable final T right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public Optional<T> left() {
    return Optional.fromNullable(left);
  }

  @Override
  public Optional<T> right() {
    return Optional.fromNullable(right);
  }

  @Override
  public void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    sb.append("left = ").append(left()).append(", ").append("right = ").append(right());
  }

  public static <T> Difference<T> fromLeftRight(@Nullable T left, @Nullable T right) {
    return new AtomicDiff<>(left, right);
  }

  public static <T> Optional<Difference<T>> diffUsingEquality(T left, T right) {
    if (left.equals(right)) {
      return Optional.absent();
    } else {
      return Optional.of(AtomicDiff.fromLeftRight(left, right));
    }
  }

  public static <T> Optional<Difference<T>> diffUsingEquality(Optional<T> left, Optional<T> right) {
    if (left.isPresent() && right.isPresent()) {
      return diffUsingEquality(left.get(), right.get());
    } else if (left.isPresent()) {
      return Optional.of(AtomicDiff.fromLeftRight(left.get(), null));
    } else if (right.isPresent()) {
      return Optional.of(AtomicDiff.fromLeftRight(null, right.get()));
    } else {
      return Optional.absent();
    }
  }
}
