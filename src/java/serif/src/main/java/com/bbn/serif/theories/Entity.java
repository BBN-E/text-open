package com.bbn.serif.theories;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.coreference.representativementions.DefaultFocusedRepresentativeMentionStrategy;
import com.bbn.serif.coreference.representativementions.DefaultRepresentativeMentionFinder;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.immutables.func.Functional;
import org.immutables.value.Value;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents some real-world entity, such as a person, place, or organization (this list is not
 * exhaustive).  The distinction between an {@link Entity} and a {@link com.bbn.serif.theories.Value}
 * can be fuzzy and ontology-specific.
 */
@TextGroupImmutable
@Value.Immutable
@JsonSerialize(as = ImmutableEntity.class)
@JsonDeserialize(as = ImmutableEntity.class)
@Functional
public abstract class Entity implements Iterable<Mention>, HasExternalID, WithEntity {

  public abstract EntityType type();

  public abstract EntitySubtype subtype();

  public abstract ImmutableMap<Mention, MentionConfidence> confidences();

  /**
   * All mentions of this entity in the document.
   */
  public abstract ImmutableSet<Mention> mentionSet();

  @Value.Default
  public boolean generic() {
    return false;
  }

  /**
   * A globally unique identifier used to coreference entities across documents.
   */
  public abstract Optional<Integer> guid();

  @Override
  public abstract Optional<Symbol> externalID();

  @Value.Check
  protected void check() {
    checkArgument(mentionSet().containsAll(confidences().keySet()),
        "Confidences provided for mentions not in this entity");
  }

  public final int numMentions() {
    return mentionSet().size();
  }

  public final Mention mention(final int idx) {
    return mentionSet().asList().get(idx);
  }


  /**
   * Returns the mention confidence for this mention in this entity. If this information was not
   * available when this Entity theory was being created, this will return {@link
   * com.bbn.serif.theories.MentionConfidence#UnknownConfidence}.
   */
  public final MentionConfidence confidence(final Mention m) {
    final MentionConfidence ret = confidences().get(m);
    if (ret != null) {
      return ret;
    } else {
      throw new SerifException(String.format(
          "Entity has no confidence for mention %s. Most likely this mention is not a member of this entity",
          m));
    }
  }

  @Override
  public final Iterator<Mention> iterator() {
    return mentionSet().iterator();
  }

  /**
   * Finds the 'most representative name' for this entity, if any, or returns {@code
   * Optional.absent()} if the entity contains no name. The algorithm used by this is subject to
   * improvements and may change. The current version may be found in {@link
   * DefaultRepresentativeMentionFinder}.
   */
  public final Optional<RepresentativeMention> representativeName() {
    final RepresentativeMention rm =
        DefaultRepresentativeMentionFinder.get().representativeMentionForEntity(this);
    if (rm.mention().isName()) {
      return Optional.of(rm);
    } else {
      return Optional.absent();
    }
  }

  /**
   * Returns the best representative mention for an entity. The algorithm returned may change as we
   * develop improvements.  The current algorithm is {@link DefaultRepresentativeMentionFinder}.
   */
  public final RepresentativeMention representativeMention() {
    return DefaultRepresentativeMentionFinder.get().representativeMentionForEntity(this);
  }

  /**
   * Returns the best representative mention for an entity while giving preference in some sense to
   * the supplied focus mention. The algorithm returned may change as we develop improvements.  The
   * current algorithm is {@link DefaultFocusedRepresentativeMentionStrategy}.
   */
  public final RepresentativeMention representativeMentionWithFocus(final Mention focusMention) {
    return DefaultFocusedRepresentativeMentionStrategy.get()
        .representativeMentionForEntity(this, focusMention);
  }

  public final boolean hasNameMention() {
    for (final Mention m : this) {
      if (m.mentionType() == Mention.Type.NAME) {
        return true;
      }
    }
    return false;
  }

  public final boolean hasDescMention() {
    for (final Mention m : this) {
      if (m.mentionType() == Mention.Type.DESC) {
        return true;
      }
    }
    return false;
  }

  public final boolean hasNameOrDescMention() {
    for (final Mention m : this) {
      if (m.mentionType() == Mention.Type.NAME
          || m.mentionType() == Mention.Type.DESC) {
        return true;
      }
    }
    return false;
  }


  public final boolean containsMention(final Mention m) {
    return mentionSet().contains(m);
  }

  @Deprecated
  public static final int UNASSIGNED_GUID = -1;

  @Deprecated
  public static final Function<? super Entity, EntityType> EntityType =
      new Function<Entity, EntityType>() {
        @Override
        public EntityType apply(final Entity e) {
          return e.type();
        }
      };


  public static Builder builder() {
    return new Builder();
  }

  @Override
  public final String toString() {
    String ret = "[" + type().toString() + "." + subtype().toString();
    if (generic()) {
      ret += " GENERIC";
    }
    if (guid().isPresent()) {
      ret += " guid=" + Integer.toString(guid().get());
    }
    ret += " ";
    ret += StringUtils.CommaSpaceJoiner.join(mentionSet());
    ret += "]";
    return ret;
  }

  public static class Builder extends ImmutableEntity.Builder {

  }

  /**
   * Represents the representative mention information for an entity. This consists of a span which
   * in some sense is judge to be most representative and the mention from which that span is
   * derived. The span is guaranteed to be a subspan of the mention and the mention is guaranteed to
   * be in the entity it was created from.  Neither the mention nor the span will be null.
   *
   * @author rgabbard
   */
  public static final class RepresentativeMention {

    private final Mention mention;
    private final Span span;

    public static RepresentativeMention from(final Entity e, final Mention m, final Span s) {
      checkArgument(e.containsMention(m));
      return new RepresentativeMention(m, s);
    }

    public static RepresentativeMention from(final Entity e, final Mention m) {
      checkArgument(e.containsMention(m));
      return new RepresentativeMention(m, m.span());
    }

    public Mention mention() {
      return mention;
    }

    public Span span() {
      return span;
    }

    private RepresentativeMention(final Mention mention, final Span span) {
      this.mention = checkNotNull(mention);
      this.span = checkNotNull(span);
      checkArgument(mention.span().contains(span));
    }
  }

  // deprecated methods

  /**
   * @deprecated - use the {@link #builder()}
   */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static Entity create(final Iterable<Mention> mentions, final EntityType type,
      final EntitySubtype entitySubtype,
      final int guid, final boolean generic, final Map<Mention, MentionConfidence> confidences) {
    return create(mentions, type, entitySubtype, guid, generic, confidences, null);
  }

  /**
   * @deprecated Prefer {@link #builder()}.
   */
  @Deprecated
  public static Entity create(final Iterable<Mention> mentions, final EntityType type,
      final EntitySubtype entitySubtype,
      final int guid, final boolean generic, final Map<Mention, MentionConfidence> confidences,
      @Nullable final Symbol externalId) {
    final Builder ret = builder().mentionSet(mentions).type(type).subtype(entitySubtype)
        .generic(generic).confidences(confidences);
    if (externalId != null) {
      ret.externalID(externalId);
    }
    if (guid != UNASSIGNED_GUID) {
      ret.guid(guid);
    }
    return ret.build();
  }

  /**
   * @deprecated Prefer {@link #generic}
   */
  @Deprecated
  public final boolean isGeneric() {
    return generic();
  }

  /**
   * @deprecated Prefer {@link #mentionSet()}
   */
  @Deprecated
  public final List<Mention> mentions() {
    return mentionSet().asList();
  }
}
