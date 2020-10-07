package com.bbn.serif.names.constraints;

import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.types.EntityType;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Indicates a name exists of the given type at the indicated character offsets.
 */
@Beta
public final class ExactNameConstraint implements NameConstraint {

  private final EntityType entityType;
  private final OffsetGroupRange offsets;
  @Nullable private final Symbol externalID;

  private ExactNameConstraint(final EntityType entityType,
      final OffsetGroupRange offsets, @Nullable final Symbol externalID) {
    this.externalID = externalID;
    this.entityType = checkNotNull(entityType);
    this.offsets = checkNotNull(offsets);
  }

  public static ExactNameConstraint create(final EntityType entityType,
      final OffsetGroupRange offsets, @Nullable final Symbol externalID) {
    return new ExactNameConstraint(entityType, offsets, externalID);
  }

  public EntityType entityType() {
    return entityType;
  }

  public OffsetGroupRange offsets() {
    return offsets;
  }

  public Optional<Symbol> externalID() {
    return Optional.fromNullable(externalID);
  }

  @Override
  public String toString() {
    return "ExactNameConstraint{" +
        "entityType=" + entityType +
        ", offsets=" + offsets +
        ", externalID=" + externalID +
        '}';
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }
}
