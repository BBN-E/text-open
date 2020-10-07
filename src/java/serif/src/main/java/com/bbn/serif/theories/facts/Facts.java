package com.bbn.serif.theories.facts;

import com.bbn.serif.theories.PotentiallyAbsentSerifTheory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Facts implements Iterable<Fact>, PotentiallyAbsentSerifTheory {

  private static Facts absent = new Facts(Collections.<Fact>emptyList());

  @Override
  public boolean isAbsent() {
    return this == Facts.absent();
  }

  public static Facts absent() {
    return absent;
  }

  private Facts(Iterable<Fact> facts) {
    this.facts = ImmutableList.copyOf(facts);
  }

  public static Facts create(Iterable<Fact> facts) {
    return new Facts(facts);
  }

  public static Facts createEmpty() {
    return create(ImmutableSet.<Fact>of());
  }

  public int size() {
    return numFacts();
  }

  public Fact get(int idx) {
    return fact(idx);
  }

  public int numFacts() {
    return facts.size();
  }

  public Fact fact(int idx) {
    return facts.get(idx);
  }

  @Override
  public Iterator<Fact> iterator() {
    return facts.iterator();
  }

  public List<Fact> asList() {
    return facts;
  }

  private final List<Fact> facts;
}
