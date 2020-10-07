package com.bbn.serif.theories.diff;

import com.bbn.bue.common.evaluation.ScoringTypedOffsetRange;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.Spannings;
import com.bbn.serif.theories.Spans;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static com.google.common.base.Functions.compose;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Determines the difference between two {@link SentenceTheory}s.  Currently only looks at event
 * mentions.
 */
@Beta
final class SentenceTheoryDiffer implements Differ<SentenceTheory, SentenceTheoryDiff> {
  private final NamesDiffer namesDiffer;
  private final EventMentionsDiffer eventMentionsDiffer;

  @Inject
  SentenceTheoryDiffer(final NamesDiffer namesDiffer, final EventMentionsDiffer eventMentionDiffer) {
    this.namesDiffer = checkNotNull(namesDiffer);
    this.eventMentionsDiffer = checkNotNull(eventMentionDiffer);
  }

  @Override
  public Optional<SentenceTheoryDiff> diff(final SentenceTheory left, final SentenceTheory right) {
    // we currently only look at event mentions!
    final Optional<CollectionDifference<List<EventMention>, EventMention, EventMentionDiff>> eventMentionDiffs =
        eventMentionsDiffer.calcEventMentionDiffs(left, right);
    final Optional<CollectionDifference<List<Name>, Name, NameDiff>> nameMentionDiffs =
        namesDiffer.calcNameDiffs(left, right);

    if (eventMentionDiffs.isPresent() || nameMentionDiffs.isPresent()) {
      return Optional.of(new SentenceTheoryDiff.Builder().left(left).right(right)
          .eventMentionDiffs(eventMentionDiffs)
          .nameDiffs(nameMentionDiffs)
          .build());
    } else {
      return Optional.absent();
    }
  }


}

class NamesDiffer {
  private final NameDiffer nameDiffer;

  @Inject
  NamesDiffer(final NameDiffer nameDiffer) {
    this.nameDiffer = checkNotNull(nameDiffer);
  }

  Optional<CollectionDifference<List<Name>, Name, NameDiff>> calcNameDiffs(
      final SentenceTheory left, final SentenceTheory right) {
    final Function<Spanning, OffsetRange<CharOffset>> NAME_SPAN_FUNCTION =
        compose(Spans.asCharOffsetRangeFunction(), Spannings.toSpanFunction());
    final ImmutableMap<OffsetRange<CharOffset>, Name> leftByOffsets =
        Maps.uniqueIndex(left.names(), NAME_SPAN_FUNCTION);
    final ImmutableMap<OffsetRange<CharOffset>, Name> rightByOffsets =
        Maps.uniqueIndex(right.names(), NAME_SPAN_FUNCTION);

    final ImmutableList.Builder<Name> leftOnlyB = ImmutableList.builder();
    final ImmutableList.Builder<Name> rightOnlyB = ImmutableList.builder();
    final ImmutableList.Builder<NameDiff> diffsB = ImmutableList.builder();

    for (final Map.Entry<OffsetRange<CharOffset>, Name> e : leftByOffsets.entrySet()) {
      if (rightByOffsets.containsKey(e.getKey())) {
        final Optional<NameDiff> nameDiff =
            nameDiffer.diff(e.getValue(), rightByOffsets.get(e.getKey()));
        if (nameDiff.isPresent()) {
          diffsB.add(nameDiff.get());
        }
      } else {
        leftOnlyB.add(e.getValue());
      }
    }

    for (final Map.Entry<OffsetRange<CharOffset>, Name> e : rightByOffsets.entrySet()) {
      if (!leftByOffsets.containsKey(e.getKey())) {
        rightOnlyB.add(e.getValue());
      }
    }

    final ImmutableList<Name> leftOnly = leftOnlyB.build();
    final ImmutableList<Name> rightOnly = rightOnlyB.build();
    final ImmutableList<NameDiff> diffs = diffsB.build();

    if (leftOnly.isEmpty() && rightOnly.isEmpty() && diffs.isEmpty()) {
      return Optional.absent();
    } else {
      // safe cast to make generics cleaner
      return Optional.of(CollectionDifference.from((List<Name>)left.names().asList(), right.names().asList(),
          leftOnly, rightOnly, diffs));
    }
  }
}

class EventMentionsDiffer {
  private static final Logger log = LoggerFactory.getLogger(EventMentionsDiffer.class);

  private final EventMentionDiffer eventMentionDiffer;

  @Inject
  EventMentionsDiffer(final EventMentionDiffer eventMentionDiffer) {
    this.eventMentionDiffer = eventMentionDiffer;
  }

  // we index event mentions by event type plus trigger offsets
  // this is not quite sufficient because nothing prevents multiple events of the same type
  // with the same offsets.  But this is good enough for now and if that case turns up
  // someone will get a nice crash to let them know there is a problem.
  enum TypedAnchorSpanFunction implements Function<EventMention, ScoringTypedOffsetRange<CharOffset>> {
    INSTANCE;

    @Override
    public ScoringTypedOffsetRange<CharOffset> apply(final EventMention input) {
      return ScoringTypedOffsetRange.create(Symbol.from("dummy"),
          input.type(), input.span().charOffsetRange());
    }
  }

  Optional<CollectionDifference<List<EventMention>, EventMention, EventMentionDiff>> calcEventMentionDiffs(
      final SentenceTheory left, final SentenceTheory right) {

    final ImmutableListMultimap<ScoringTypedOffsetRange<CharOffset>, EventMention> leftByAnchor =
        Multimaps.index(left.eventMentions(), TypedAnchorSpanFunction.INSTANCE);
    final ImmutableListMultimap<ScoringTypedOffsetRange<CharOffset>, EventMention> rightByAnchor =
        Multimaps.index(right.eventMentions(), TypedAnchorSpanFunction.INSTANCE);

    final ImmutableList.Builder<EventMention> leftOnlyB = ImmutableList.builder();
    final ImmutableList.Builder<EventMention> rightOnlyB = ImmutableList.builder();
    final ImmutableList.Builder<EventMentionDiff> diffsB = ImmutableList.builder();

    for (final Map.Entry<ScoringTypedOffsetRange<CharOffset>, Collection<EventMention>> e : leftByAnchor.asMap().entrySet()) {
      if (e.getValue().size() > 1 || rightByAnchor.get(e.getKey()).size() > 1) {
        log.warn("Multiple events match fingerprint {}, cannot diff", e.getKey());
      } else {
        final EventMention leftEventMention = Iterables.getOnlyElement(e.getValue());
        if (rightByAnchor.containsKey(e.getKey())) {
          final EventMention rightEventMention = Iterables.getOnlyElement(rightByAnchor.get(e.getKey()));
          final Optional<EventMentionDiff> eventMentionDiff =
              eventMentionDiffer.diff(leftEventMention, rightEventMention);
          if (eventMentionDiff.isPresent()) {
            diffsB.add(eventMentionDiff.get());
          }
        } else {
          leftOnlyB.add(leftEventMention);
        }
      }
    }

    for (final Map.Entry<ScoringTypedOffsetRange<CharOffset>, Collection<EventMention>> e : rightByAnchor.asMap().entrySet()) {
      if (!leftByAnchor.containsKey(e.getKey())) {
        rightOnlyB.addAll(e.getValue());
      }
    }

    final ImmutableList<EventMention> leftOnly = leftOnlyB.build();
    final ImmutableList<EventMention> rightOnly = rightOnlyB.build();
    final ImmutableList<EventMentionDiff> diffs = diffsB.build();

    if (leftOnly.isEmpty() && rightOnly.isEmpty() && diffs.isEmpty()) {
      return Optional.absent();
    } else {
      return Optional.of(CollectionDifference.from(left.eventMentions().asList(), right.eventMentions().asList(),
          leftOnly, rightOnly, diffs));
    }
  }
}
