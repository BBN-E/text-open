package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.theories.Proposition.MentionArgument;
import com.bbn.serif.theories.Proposition.PredicateType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.immutables.func.Functional;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

@JsonSerialize(as = ImmutablePropositions.class)
@JsonDeserialize(as = ImmutablePropositions.class)
@TextGroupImmutable
@org.immutables.value.Value.Immutable
@Functional
public abstract class Propositions implements Iterable<Proposition>, PotentiallyAbsentSerifTheory {

  public abstract ImmutableSet<Proposition> propositions();

  public abstract Optional<Mentions> mentions();

  @org.immutables.value.Value.Default
  @Override
  public boolean isAbsent() {
    return false;
  }

  @org.immutables.value.Value.Check
  protected void check() {
    checkArgument(!isAbsent() || propositions().isEmpty());
    if (mentions().isPresent()) {
      checkArgument(
          mentions().get().mentions().containsAll(mentionToPropositionArguments().keySet()));
    } else {
      checkArgument(isAbsent(), "Mentions element may only be present for absent Propositions");
    }
  }

  public static Propositions absent() {
    return new Builder().isAbsent(true)
        .build();
  }

  public final Proposition proposition(int idx) {
    return propositions().asList().get(idx);
  }

  public final int numPropositions() {
    return propositions().size();
  }


  public final int size() {
    return numPropositions();
  }

  public final List<Proposition> asList() {
    return propositions().asList();
  }

  public final Proposition get(int idx) {
    return proposition(idx);
  }

  @Override
  public final Iterator<Proposition> iterator() {
    return propositions().iterator();
  }

  public final Optional<Proposition> findPropositionByNode(SynNode node) {
    for (Proposition prop : this) {
      Optional<SynNode> predHead = prop.predHead();
      if (predHead.isPresent()) {
        if (predHead.get() == node) {
          return Optional.of(prop);
        } else {
          final SynNode predHeadPreterm = predHead.get().headPreterminal();
          if (predHeadPreterm == node) {
            return Optional.of(prop);
          }
        }
      }
    }
    return Optional.absent();
  }

  public final Optional<Proposition> definition(Mention m) {
    return Optional.fromNullable(definitions().get(m));
  }

  public final Collection<Proposition.MentionArgument> propositionArgumentsFor(Mention m) {
    return mentionToPropositionArguments().get(m);
  }

  private static final Predicate<Proposition> ModifierWithTwoArgs =
      Predicates.and(Proposition.OfType(PredicateType.MODIFIER),
          Proposition.AtLeastNArgs(2));
  private static final ImmutableList<Predicate<Proposition>> definitionPriorities =
      ImmutableList.of(
          Proposition.OfType(
              ImmutableList.of(PredicateType.NOUN, PredicateType.PRONOUN, PredicateType.SET)),
          Proposition.OfType(PredicateType.NAME),
          ModifierWithTwoArgs,
          Proposition.OfType(PredicateType.MODIFIER));


  @org.immutables.value.Value.Derived
  ImmutableMap<Mention, Proposition> definitions() {
    ImmutableMap.Builder<Mention, Proposition> definitions = ImmutableMap.builder();
    Set<Mention> definedMentions = Sets.newHashSet();

    for (final Predicate<Proposition> filter : definitionPriorities) {
      for (final Proposition prop : propositions()) {
        if (!prop.args().isEmpty() && prop.arg(0) instanceof Proposition.MentionArgument) {
          final MentionArgument mentionArg = (Proposition.MentionArgument) prop.arg(0);
          final Mention m = mentionArg.mention();
          if (filter.apply(prop) && !definedMentions.contains(m)) {
            definedMentions.add(m);
            definitions.put(m, prop);
          }
        }
      }
    }

    return definitions.build();
  }

  @org.immutables.value.Value.Derived
  ImmutableMultimap<Mention, Proposition.MentionArgument> mentionToPropositionArguments() {
    final ImmutableMultimap.Builder<Mention, Proposition.MentionArgument> ret =
        ImmutableMultimap.builder();

    for (final Proposition prop : propositions()) {
      for (final Proposition.Argument arg : prop.args()) {
        if (arg instanceof MentionArgument) {
          final MentionArgument ma = (MentionArgument) arg;
          ret.put(ma.mention(), ma);
        }
      }
    }
    return ret.build();
  }

  public static class Builder extends ImmutablePropositions.Builder {

  }
}
