package com.bbn.serif.types;

import com.bbn.bue.common.HasStableHashCode;
import com.bbn.bue.common.ModuleFromParameter;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.symbols.Symbol;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.lang.annotation.Annotation;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents the type of some entity, such as a person or organization.  These types are usually
 * associated with {@link com.bbn.serif.theories.Name} and {@link com.bbn.serif.theories.Entity}
 * objects.
 *
 * Equality and hash codes of {@code EntityType}s is based entirely on the name.  Two entity types
 * have special significance for documents originating from CSerif:
 * {@code UNDET} indicating that no entity type has been assigned and {@code OTH} indicating an
 * entity type outside the ontology has been assigned.
 *
 * Constants for some common types from the ACE, ERE, and Lorelei ontologies are provided for
 * convenience.  Beware that the annotation guidelines for even the same type may differ
 * for even the same type from data source to data source. If you'd like to allow the user to
 * specify a set of entity types from a param file
 * for some purpose, please see the Javadoc for {@link EntityOntologyM}.
 */
@TextGroupImmutable
// we used to use intern=true, but it led to
// initialization problems
@Value.Immutable
@JsonSerialize(as = ImmutableEntityType.class)
@JsonDeserialize(as = ImmutableEntityType.class)
@Functional
public abstract class EntityType implements HasStableHashCode {

  /**
   * The name of the entity type, which entirely determines its identity.
   */
  public abstract Symbol name();

  /**
   * Creates an entity type with the given name.
   */
  public static EntityType of(Symbol name) {
    return ImmutableEntityType.builder().name(name).build();
  }

  /**
   * Creates an entity type with the given name.
   */
  public static EntityType of(String name) {
    return of(Symbol.from(name));
  }

  /**
   * Gets CSerif's undetermined entity type, {@code UNDET}
   */
  public static EntityType undetermined() {
    return UNDET;
  }

  @Override
  public final String toString() {
    return name().toString();
  }

  public final int stableHashCode() { return name().stableHashCode(); }

  private static final EntityType UNDET = EntityType.of("UNDET");
  private static final EntityType OTH = EntityType.of("OTH");

  @Deprecated
  public boolean isNotUndetOrOth() {
    return !this.equals(UNDET) && !this.equals(OTH);
  }

  /**
   * ACE/ERE/Lorelei Person type.
   */
  public static final EntityType PER = EntityType.of("PER");

  /**
   * ACE/ERE/Lorelei Organization type.
   */
  public static final EntityType ORG = EntityType.of("ORG");

  /**
   * ACE/ERE/Lorelei Location type.
   */
  public static final EntityType LOC = EntityType.of("LOC");

  /**
   * ACE/ERE/Lorelei Geo-political entity type.
   */
  public static final EntityType GPE = EntityType.of("GPE");
  /**
   * ACE Weapon type.
   */
  public static final EntityType WEA = EntityType.of("WEA");

  /**
   * ACE Vehicle type.
   */
  public static final EntityType VEH = EntityType.of("VEH");

  /**
   * ACE/ERE Facility type.
   */
  public static final EntityType FAC = EntityType.of("FAC");

  /**
   * MUC Title type.
   */
  public static final EntityType TTL = EntityType.of("TTL");

  private static final ImmutableSet<EntityType> ACE_ENTITY_TYPES = ImmutableSet.of(
      PER, ORG, GPE, LOC, FAC, WEA, VEH);

  /**
   * Entity types used in the ACE program. This is CSerif's standard entity type set for English.
   */
  public static ImmutableSet<EntityType> aceTypes() {
    return ACE_ENTITY_TYPES;
  }

  private static final ImmutableSet<EntityType> LORELEI_ENTITY_TYPES = ImmutableSet.of(
    PER, ORG, GPE, LOC);

  /**
   * Entity types used in the DARPA Lorelei program.
   */
  public static ImmutableSet<EntityType> loreleiTypes() {
    return LORELEI_ENTITY_TYPES;
  }

  private static final ImmutableSet<EntityType> MUC_ENTITY_TYPES = ImmutableSet.of(PER, ORG,
      LOC, TTL);

  /**
   * Entity types from the old MUC evals.
   */
  public static ImmutableSet<EntityType> mucTypes() {
    return MUC_ENTITY_TYPES;
  }

  /**
   * Allows the user to specify an event ontology for some purpose using a parameter file. When
   * installing this module, you need
   */
  public static class EntityOntologyM extends PrivateModule {
    private final String ontologyParam;
    private final Class<? extends Annotation> annotationType;
    private final Parameters params;

    public EntityOntologyM(final Parameters parameters,
        final String ontologyParam, Class<? extends Annotation> annotationType) {
      this.params = checkNotNull(parameters);
      this.ontologyParam = checkNotNull(ontologyParam);
      this.annotationType = checkNotNull(annotationType);
      checkArgument(!ontologyParam.isEmpty());
    }

    @Override
    public void configure() {
      // we call to this even though we don't use it so Guice knows to set up multibinding for
      // EntityTypes so we don't get a crash even if nothing is bound
      entityTypeMultibinder(binder());
      install(ModuleFromParameter.forMultiParameter(ontologyParam).extractFrom(params));
      final Key<Set<EntityType>> externalKey = Key.get(new TypeLiteral<Set<EntityType>>() {
      }, annotationType);
      bind(externalKey).to(new TypeLiteral<Set<EntityType>>() {});
      expose(externalKey);
    }

    public static Multibinder<EntityType> entityTypeMultibinder(Binder binder) {
      return Multibinder.newSetBinder(binder, EntityType.class);
    }
  }

  public static class AceOntologyM extends AbstractModule {
    @Override
    protected void configure() {
      final Multibinder<EntityType> multibinder = EntityOntologyM.entityTypeMultibinder(binder());

      for (final EntityType entityType : aceTypes()) {
        multibinder.addBinding().toInstance(entityType);
      }
    }
  }

  public static class LoreleiOntologyM extends AbstractModule {
    @Override
    protected void configure() {
      final Multibinder<EntityType> multibinder = EntityOntologyM.entityTypeMultibinder(binder());

      for (final EntityType entityType : loreleiTypes()) {
        multibinder.addBinding().toInstance(entityType);
      }
    }
  }

  public static class MucOntologyM extends AbstractModule {
    @Override
    protected void configure() {
      final Multibinder<EntityType> multibinder = EntityOntologyM.entityTypeMultibinder(binder());

      for (final EntityType entityType : mucTypes()) {
        multibinder.addBinding().toInstance(entityType);
      }
    }
  }
}

