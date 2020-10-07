package com.bbn.serif.constraints;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.entities.constraints.EntityConstraint;
import com.bbn.serif.events.constraints.EventConstraint;
import com.bbn.serif.events.constraints.EventMentionConstraint;
import com.bbn.serif.mentions.constraints.MentionConstraint;
import com.bbn.serif.names.constraints.NameConstraint;
import com.bbn.serif.parse.constraints.ParseConstraint;
import com.bbn.serif.regions.constraints.RegionConstraint;
import com.bbn.serif.relations.constraints.RelationConstraint;
import com.bbn.serif.relations.constraints.RelationMentionConstraint;
import com.bbn.serif.sentences.constraints.SentenceSegmentationConstraint;
import com.bbn.serif.tokens.constraints.TokenizationConstraint;
import com.bbn.serif.values.constraints.ValueMentionConstraint;

import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

@Value.Immutable
@Functional
@TextGroupImmutable
public abstract class SerifConstraints implements WithSerifConstraints {

  public abstract Symbol docID();
  public abstract ImmutableSet<RegionConstraint> regionConstraints();
  public abstract ImmutableSet<SentenceSegmentationConstraint> segmentationConstraints();
  public abstract ImmutableSet<TokenizationConstraint> tokenizationConstraints();
  public abstract ImmutableSet<EntityConstraint> entityConstraints();
  public abstract ImmutableSet<MentionConstraint> mentionConstraints();
  public abstract ImmutableSet<RelationMentionConstraint> relationMentionConstraints();
  public abstract ImmutableSet<RelationConstraint> relationConstraints();
  public abstract ImmutableSet<NameConstraint> nameConstraints();
  public abstract ImmutableSet<ValueMentionConstraint> valueConstraints();
  public abstract ImmutableSet<ParseConstraint> parseConstraints();
  public abstract ImmutableSet<EventMentionConstraint> eventMentionConstraints();
  public abstract ImmutableSet<EventConstraint> eventConstraints();

  public static class Builder extends ImmutableSerifConstraints.Builder {}
}
