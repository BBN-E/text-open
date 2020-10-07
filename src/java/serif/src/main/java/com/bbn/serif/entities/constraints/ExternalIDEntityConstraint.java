package com.bbn.serif.entities.constraints;


import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Beta
public final class ExternalIDEntityConstraint implements EntityConstraint {

  private final boolean generic;
  private final EntityType entityType;
  private final EntitySubtype entitySubtype;
  private final Symbol externalID;
  private final ImmutableSet<Symbol> mentionIDs;

  private ExternalIDEntityConstraint(final boolean generic, final EntityType entityType,
      final EntitySubtype entitySubtype, final Symbol externalID, final Set<Symbol> mentionIDs) {
    this.generic = generic;
    this.externalID = checkNotNull(externalID);
    this.mentionIDs = ImmutableSet.copyOf(mentionIDs);
    this.entityType = checkNotNull(entityType);
    this.entitySubtype = checkNotNull(entitySubtype);
  }

  public static ExternalIDEntityConstraint create(final boolean generic, final EntityType entityType,
      final EntitySubtype entitySubtype, final Symbol externalID, final Set<Symbol> mentionIDs) {
    return new ExternalIDEntityConstraint(generic, entityType, entitySubtype, externalID, mentionIDs);
  }

  public boolean generic() {
    return generic;
  }

  public EntityType entityType() {
    return entityType;
  }

  public EntitySubtype entitySubtype() {
    return entitySubtype;
  }

  public Symbol externalID() {
    return externalID;
  }

  public ImmutableSet<Symbol> mentionIDs() {
    return mentionIDs;
  }

  @Override
  public boolean satisfiedBy(final DocTheory dt) {
    // TODO implement this
    return true;
  }
}
