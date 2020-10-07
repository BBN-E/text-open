package com.bbn.serif.theories.diff;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

@Beta
final class DiffUtils {
  private DiffUtils() {
    throw new UnsupportedOperationException();
  }

  public static <DiffType extends Difference<?>> void writePropertyDiff(String name,
      Optional<DiffType> diff, final StringBuilder sb, int indent) {
    if (diff.isPresent()) {
      sb.append(Strings.repeat(" ", indent)).append(name).append(" : ");
      diff.get().writeTextReport(sb, indent+2);
      sb.append("\n");
    }
  }
}
