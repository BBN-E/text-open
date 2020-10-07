package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupPublicImmutable;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMention;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A representation of an event at a document level. This is patterned closely after
 * Adept's {@code DocumentEvent}.  This class it itself just a template; users should use
 * the generated {@link DocumentEvent} class.
 */
@SuppressWarnings("deprecation")
@TextGroupPublicImmutable
@Value.Immutable
@Value.Enclosing
abstract class _DocumentEvent implements HasExternalID {

  /**
   * The type of the event. The event type is prefixed with "primary" to leave open the option
   *   of supporting multi-typed events in the future.
   */
  @Value.Parameter(order=0)
  public abstract Symbol primaryType();

  /**
   * Textual evidence for the occurrence of an event.  This may or may not be populated.
   * Most users will only care about the values of this map. It is a map rather than a list
   * because the TAC KBP evaluations distinguish multiple types of justifications.
   */
  public abstract ImmutableListMultimap<Symbol, LocatedString> justifications();

  /**
   * "Machine-readable" evidence for the occurrence of an event. This may or may
   * not be populated. While {@link #justifications()} consists of strings meant for
   * human consumption, this consists of e.g. Serif objects meant for algorithmic
   * consumption.
   */
  public abstract ImmutableList<DocumentEvent.Provenance> provenances();

  /**
   * The arguments of the event.  This doesn't have to be populated but almost always should
   * be.
   */
  public abstract ImmutableList<DocumentEvent.Argument> arguments();

  /**
   * Allows the association of arbitrary scored attributes with events (e.g. genericity).
   */
  public abstract ImmutableMap<Symbol, Double> scoredAttributes();

  /**
   * A score assigned to this event.
   */
  public abstract Optional<Double> score();

  @Override
  public abstract Optional<Symbol> externalID();


  /**
   * The evidence for the occurrence of an event in an form intended for algorithm consumption.
   */
  public interface Provenance {
  }

  /**
   * Indicates a {@link DocumentEvent} originates from a Serif {@link EventMention}.
   */
  @Value.Immutable
  public static abstract class EventMentionProvenance implements Provenance {
    @Value.Parameter(order=1)
    public abstract EventMention eventMention();
  }

  /**
   * Indicates a {@link DocumentEvent} originates from a Serif {@link FlexibleEventMention}.
   */
  @Value.Immutable
  public interface FlexibleEventMentionProvenance extends Provenance {
    @Value.Parameter(order=1)
    FlexibleEventMention flexibleEventMention();
  }

  /**
   * Indicates a {@link DocumentEvent} originates from some arbitrary text.
   */
  @Value.Immutable
  public static abstract class TextualProvenance implements Provenance {
    @Value.Parameter(order=1)
    public abstract ImmutableList<LocatedString> locatedStrings();

    @Value.Check
    protected void checkPreconditions() {
      checkArgument(!locatedStrings().isEmpty(),
          "TextualProvenance must have at least one LocatedString");
    }
  }

  /**
   * The argument of a document-level event.
   */
  @Value.Immutable
  public interface Argument extends HasMetadata, HasExternalID {
    @Value.Parameter(order=1)
    Symbol type();

    @Value.Parameter(order = 2)
    Symbol role();
    /**
     * The entity playing {@link #role()} in the event.
     */
    @Value.Parameter(order = 3)
    ArgumentFiller filler();

    @Override
    ImmutableMap<Symbol, String> metadata();

    /**
     * Algorithm-consumable evidence for that {@link #filler()}  plays {@link #role()} in this event.
     * This may or may not be populated. **/
    ImmutableList<ArgumentProvenance> provenances();

    /**
     * Textual evidence for that {@link #filler()}  plays {@link #role()} in this event.
     * This may or may not be populated.
     * Most users will only care about the values of this map. It is a map rather than a list
     * because the TAC KBP evaluations distinguish multiple types of justifications.
     */
    ImmutableMultimap<Symbol, LocatedString> justifications();

    /**
     * Allows the association of arbitrary scored attributes with event arguments (e.g. genericity).
     */
    ImmutableMap<Symbol, Double> scoredAttributes();
    Optional<Double> score();

    @Override
    Optional<Symbol> externalID();

    interface ArgumentProvenance {

    }

    @Value.Immutable
    interface EventMentionArgumentProvenance extends ArgumentProvenance {
      Optional<EventMention> eventMention();
      @Value.Parameter(order=1)
      EventMention.Argument argument();
    }

    interface ArgumentFiller {
      Optional<String> canonicalString();

      Optional<OffsetGroupRange> canonicalStringOffsets();
    }

    @Value.Immutable
    abstract class EntityFiller implements ArgumentFiller {
      @Value.Parameter(order=1)
      public abstract Entity entity();
    }

    @Value.Immutable
    abstract class TextFiller implements ArgumentFiller {
      @Value.Parameter(order=1)
      public abstract LocatedString text();
    }

    @Value.Immutable
    abstract class ValueFiller implements ArgumentFiller {

      @Value.Parameter(order = 1)
      public abstract com.bbn.serif.theories.Value value();
    }

    /**
     * This is intended only for {@link ValueMention} arguments not resolvable to {@link
     * com.bbn.serif.theories.Value}s. Otherwise prefer {@link ValueFiller}.
     */
    @Value.Immutable
    abstract class ValueMentionFiller implements ArgumentFiller {

      @Value.Parameter(order = 1)
      public abstract ValueMention valueMention();
    }
  }
}
