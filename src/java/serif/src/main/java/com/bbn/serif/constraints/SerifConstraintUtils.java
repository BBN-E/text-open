package com.bbn.serif.constraints;

import com.bbn.bue.common.symbols.Symbol;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.common.collect.Iterables.transform;

public final class SerifConstraintUtils {

  private SerifConstraintUtils() {
    throw new UnsupportedOperationException();
  }

  public static SerifConstraints combine(SerifConstraints first, SerifConstraints... others) {
    final List<SerifConstraints> constraints = new ArrayList<>();
    constraints.add(first);
    constraints.addAll(Arrays.asList(others));
    return combine(constraints);
  }

  public static SerifConstraints combine(Iterable<SerifConstraints> constraints) {
    final Symbol docID =
        Iterables.getOnlyElement(
            ImmutableSet.copyOf(transform(constraints, SerifConstraintsFunctions.docID())));
    final SerifConstraints.Builder ret = new SerifConstraints.Builder();
    ret.docID(docID);
    for (final SerifConstraints constraint : constraints) {
      ret.addAllEntityConstraints(constraint.entityConstraints())
          .addAllEventConstraints(constraint.eventConstraints())
          .addAllEventMentionConstraints(constraint.eventMentionConstraints())
          .addAllMentionConstraints(constraint.mentionConstraints())
          .addAllNameConstraints(constraint.nameConstraints())
          .addAllParseConstraints(constraint.parseConstraints())
          .addAllRegionConstraints(constraint.regionConstraints())
          .addAllRelationConstraints(constraint.relationConstraints())
          .addAllRelationMentionConstraints(constraint.relationMentionConstraints())
          .addAllSegmentationConstraints(constraint.segmentationConstraints())
          .addAllTokenizationConstraints(constraint.tokenizationConstraints())
          .addAllValueConstraints(constraint.valueConstraints());
    }
    return ret.build();
  }

}
