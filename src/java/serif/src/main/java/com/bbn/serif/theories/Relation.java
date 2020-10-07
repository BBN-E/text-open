package com.bbn.serif.theories;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Tense;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public final class Relation implements Iterable<RelationMention>, HasExternalID {

  public Entity leftEntity() {
    return leftEntity;
  }

  public Entity rightEntity() {
    return rightEntity;
  }

  public Tense tense() {
    return tense;
  }

  public Symbol type() {
    return type;
  }

  public Modality modality() {
    return modality;
  }

  public Float confidence() {
    return confidence;
  }

  @Override
  public Optional<Symbol> externalID() {
    return Optional.fromNullable(external_id);
  }

  public List<RelationMention> relationMentions() {
    return relationMentions;
  }

  @Override
  public Iterator<RelationMention> iterator() {
    return relationMentions.iterator();
  }

  private final List<RelationMention> relationMentions;
  private final Entity leftEntity;
  private final Entity rightEntity;
  private final Tense tense;
  private final Symbol type;
  private final Modality modality;
  private final Float confidence;
  @Nullable
  private final Symbol external_id;

  /**
   * This will be removed in a future version. Prefer {@link #create(Entity, Entity, RelationMentions, Symbol, Tense, Modality, float, Symbol)}
   */
  @Deprecated
  public Relation(Entity leftEntity, Entity rightEntity, RelationMentions mentions,
      Symbol type, Tense tense, Modality modality, float confidence) {
    this(leftEntity, rightEntity, mentions, type, tense, modality, confidence, null);
  }

  /**
   * This will be removed in a future version. Prefer {@link #create(Entity, Entity,
   * RelationMentions, Symbol, Tense, Modality, float, Symbol)}
   */
  @Deprecated
  public Relation(Entity leftEntity, Entity rightEntity, RelationMentions mentions,
      Symbol type, Tense tense, Modality modality, float confidence,
      @Nullable final Symbol external_id) {
    this.external_id = external_id;
    this.relationMentions = ImmutableList.copyOf(mentions);
    this.leftEntity = checkNotNull(leftEntity);
    this.rightEntity = checkNotNull(rightEntity);
    this.type = checkNotNull(type);
    this.tense = checkNotNull(tense);
    this.modality = checkNotNull(modality);
    this.confidence = confidence;
  }

  public static Relation create(Entity leftEntity, Entity rightEntity, RelationMentions mentions,
      Symbol type, Tense tense, Modality modality, float confidence,
      @Nullable final Symbol external_id) {
    return new Relation(leftEntity, rightEntity, mentions, type, tense, modality, confidence,
        external_id);
  }
}
