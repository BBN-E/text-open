package com.bbn.serif.mentions.constraints;


import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

/**
 * Indicates a mention of a given type, entity type, exist at the given offsets
 */
@Beta
public final class ExactMentionConstraint implements MentionConstraint {

  private final EntityType entityType;
  private final EntitySubtype entitySubtype;
  private final Mention.Type mentionType;
  private final OffsetGroupRange offsets;
  private final Optional<OffsetGroupRange> head;
  @Nullable
  private final Symbol externalID;

  private ExactMentionConstraint(final EntityType entityType, final EntitySubtype entitySubtype,
      final Mention.Type mentionType, final OffsetGroupRange offsets,
      final Optional<OffsetGroupRange> head, @Nullable final Symbol externalID) {
    this.entityType = entityType;
    this.entitySubtype = entitySubtype;
    this.mentionType = mentionType;
    this.offsets = offsets;
    this.head = head;
    this.externalID = externalID;
  }

  public static ExactMentionConstraint create(final EntityType entityType,
      final EntitySubtype entitySubtype,
      final Mention.Type mentionType, final OffsetGroupRange offsets,
      final Optional<OffsetGroupRange> head, @Nullable final Symbol externalID) {
    return new ExactMentionConstraint(entityType, entitySubtype, mentionType, offsets, head,
        externalID);
  }

  public EntityType entityType() {
    return entityType;
  }

  public EntitySubtype entitySubtype() {
    return entitySubtype;
  }

  public Mention.Type mentionType() {
    return mentionType;
  }

  public OffsetGroupRange offsets() {
    return offsets;
  }

  public Optional<OffsetGroupRange> head() {
    return head;
  }

  public Optional<Symbol> externalID() {
    return Optional.fromNullable(externalID);
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }

  @Override
  public String toString() {
    final MoreObjects.ToStringHelper ret = MoreObjects.toStringHelper(this)
        .add("type", entityType)
        .add("subType", entitySubtype)
        .add("mType", mentionType)
        .add("offsets", offsets);
    if (head.isPresent()) {
      ret.add("head", head.get());
    }
    if (externalID != null) {
      ret.add("externalID", externalID);
    }
    return ret.toString();
  }
}
