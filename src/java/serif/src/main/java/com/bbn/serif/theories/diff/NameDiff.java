package com.bbn.serif.theories.diff;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Name;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import org.immutables.func.Functional;
import org.immutables.value.Value;

/**
 * A description of the difference between two {@link Name}s
 */
@Value.Immutable
@Functional
@TextGroupImmutable
public abstract class NameDiff implements Difference<Name> {
  @Value.Parameter
  @Override
  public abstract Optional<Name> left();
  @Value.Parameter
  @Override
  public abstract Optional<Name> right();

  public abstract Optional<Difference<Symbol>> typeDiff();
  public abstract Optional<Difference<Double>> scoreDiff();

  @Override
  public final void writeTextReport(final StringBuilder sb, final int indentSpaces) {
    sb.append(Strings.repeat(" ", indentSpaces)).append("Name:\n");
    if (typeDiff().isPresent()) {
      DiffUtils.writePropertyDiff("type", typeDiff(), sb, indentSpaces+2);
    }
    if (scoreDiff().isPresent()) {
      DiffUtils.writePropertyDiff("score", scoreDiff(), sb, indentSpaces+2);
    }
  }

  public static class Builder extends ImmutableNameDiff.Builder {}
}
