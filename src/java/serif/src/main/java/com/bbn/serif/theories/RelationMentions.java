package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@JsonSerialize
@JsonDeserialize
@TextGroupImmutable
@Value.Immutable
public abstract class RelationMentions
    implements Iterable<RelationMention>, PotentiallyAbsentSerifTheory {

  public abstract ImmutableSet<RelationMention> relationMentions();

  @Value.Default
  @Override
  public boolean isAbsent() {
    return false;
  }

  public static RelationMentions absent() {
    return new RelationMentions.Builder().isAbsent(true).build();
  }

  public static RelationMentions create(Iterable<RelationMention> relationMentions) {
    return new Builder().relationMentions(relationMentions).build();
  }

  public final int numRelationMentions() {
    return relationMentions().size();
  }

  public final RelationMention relationMention(int idx) {
    return asList().get(idx);
  }

  public final int size() {
    return numRelationMentions();
  }

  public final RelationMention get(int idx) {
    return relationMention(idx);
  }

  @Override
  public final Iterator<RelationMention> iterator() {
    return relationMentions().iterator();
  }

  public final List<RelationMention> asList() {
    return relationMentions().asList();
  }

  @Value.Check
  protected void check() {
    checkArgument(!isAbsent() || relationMentions().isEmpty(),
        "Absent relation mentions must be empty");
  }


  public static class Builder extends ImmutableRelationMentions.Builder {

  }
}
