package com.bbn.serif.theories;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Values implements Iterable<Value>, PotentiallyAbsentSerifTheory {

  private static Values absent = new Values(Collections.<Value>emptyList());

  @Override
  public boolean isAbsent() {
    return this == Values.absent();
  }

  public static Values absent() {
    return absent;
  }

  public static Values create(Iterable<Value> values) {
    return new Values(values);
  }

  public static Values createEmpty() {
    return create(ImmutableList.<Value>of());
  }

  /**
   * Not really deprecated, but its use as a public method is.
   */
  @Deprecated
  public Values(Iterable<Value> values) {
    this.values = ImmutableList.copyOf(values);
  }

  public int numValues() {
    return values.size();
  }

  public Value value(int idx) {
    return values.get(idx);
  }

  public int size() {
    return numValues();
  }

  public Value get(int idx) {
    return value(idx);
  }

  @Override
  public Iterator<Value> iterator() {
    return values.iterator();
  }

  public List<Value> asList() {
    return values;
  }

  private final List<Value> values;

  public Optional<Value> getValueByValueMention(ValueMention valueMention) {
    checkNotNull(valueMention);
    for (Value value : this.asList()) {
      if (value.valueMention().equals(valueMention)) {
        return Optional.of(value);
      }
    }
    return Optional.of(null);
  }
}
