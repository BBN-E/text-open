package com.bbn.serif.types;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.immutables.value.Value;

/**
 * More specific information about the type of a {@link com.bbn.serif.theories.Name}
 * or {@link com.bbn.serif.theories.Entity}.  An {@code EntitySubtype} can only be interpreted
 * in the context of the corresponding {@link EntityType} of the object.
 *
 * The identity of an entity subtype for purposes of equality and hashcode is determined
 * entirely by its name.
 *
 * In Serif documents originating from CSerif, the special type {@code UNDET} is used to
 * indicate an unassigned subtype.  Users can check for this case using {@link #isUndetermined()}.
 */
@TextGroupImmutable
// we used to use intern=true, but it led to
// initialization problems
@Value.Immutable
@JsonSerialize(as = ImmutableEntitySubtype.class)
@JsonDeserialize(as = ImmutableEntitySubtype.class)
public abstract class EntitySubtype {

  /**
   * The name of this entity subtype.
   */
  public abstract Symbol name();

  /**
   * Creates an entity subtype with the given name.
   */
  public static EntitySubtype of(final Symbol name) {
    return ImmutableEntitySubtype.builder().name(name).build();
  }

  /**
   * Creates an entity subtype with the given name.
   */
  public static EntitySubtype of(final String name) {
    return ImmutableEntitySubtype.builder().name(Symbol.from(name)).build();
  }

  private static final EntitySubtype UNDET = EntitySubtype.of("UNDET");

  /**
   * Gets the special entity subtype {@code UNDET} used by CSerif to indicate an unassigned subtype.
   */
  public static EntitySubtype undetermined() {
    return UNDET;
  }

  /**
   * Tests whether this entity subtype is the special type {@code UNDET} used by CSerif to
   * indicate an unassigned subtype.
   */
  public boolean isUndetermined() {
    return equals(UNDET);
  }

  @Override
  public String toString() {
    return name().toString();
  }
}
