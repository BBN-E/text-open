package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;


@JsonSerialize(as = ImmutableNestedNames.class)
@JsonDeserialize(as = ImmutableNestedNames.class)
@Functional
@TextGroupImmutable
@Value.Immutable(prehash = true)
public abstract class NestedNames implements Iterable<NestedName>, WithNestedNames {

  public abstract TokenSequence tokenSequence();

  public abstract Names parent();

  public abstract ImmutableSet<NestedName> names();


  public abstract Optional<Double> score();

  public final NestedName nestedName(int idx) {
    return names().asList().get(idx);
  }

  public final int size() {
    return names().size();
  }

  @Override
  public Iterator<NestedName> iterator() {
    return names().iterator();
  }

  public List<NestedName> asList() {
    return names().asList();
  }

  public static class Builder extends ImmutableNestedNames.Builder {

  }
}
