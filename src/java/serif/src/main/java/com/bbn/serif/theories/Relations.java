package com.bbn.serif.theories;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Relations implements Iterable<Relation>, PotentiallyAbsentSerifTheory {

  private static Relations absent = new Relations(Collections.<Relation>emptyList());

  @Override
  public boolean isAbsent() {
    return this == Relations.absent();
  }

  public static Relations absent() {
    return absent;
  }

  public static Relations create(Iterable<Relation> relations) {
    return new Relations(relations);
  }

  public static Relations createEmpty() {
    return new Relations(ImmutableSet.<Relation>of());
  }

  /**
   * Not really deprecated, but its use as a public method is
   */
  @Deprecated
  public Relations(Iterable<Relation> relations) {
    this.relations = ImmutableList.copyOf(relations);
  }

  public int numRelations() {
    return relations.size();
  }

  public Relation relation(int idx) {
    return relations.get(idx);
  }

  @Override
  public Iterator<Relation> iterator() {
    return relations.iterator();
  }

  public List<Relation> asList() {
    return relations;
  }

  private final List<Relation> relations;
}
