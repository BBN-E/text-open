package com.bbn.serif.coreference.representativementions;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Entity.RepresentativeMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.Spannings;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import static com.bbn.serif.theories.Mention.OfType;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Deprecated in favor of {@link com.bbn.serif.coreference.representativementions.DefaultFocusedRepresentativeMentionStrategy}
 */
@Deprecated
public class DefaultFocusedRepresentativeMentionFinder implements RepresentativeMentionFinder {

  private final Mention focusMention;

  private DefaultFocusedRepresentativeMentionFinder(final Mention focusMention) {
    this.focusMention = focusMention;
  }

  public static DefaultFocusedRepresentativeMentionFinder createWithFocusMention(
      final Mention focusMention) {
    return new DefaultFocusedRepresentativeMentionFinder(focusMention);
  }

  @Override
  public RepresentativeMention representativeMentionForEntity(final Entity e) {
    checkArgument(e.containsMention(focusMention));
    if (e.hasNameMention()) {
      return DefaultRepresentativeMentionFinder.get().representativeMentionForEntity(e);
    } else if (e.hasDescMention()) {
      return bestDescMention(e, focusMention);
    } else {
      return RepresentativeMention.from(e, focusMention);
    }
  }

  private static RepresentativeMention bestDescMention(final Entity e, final Mention focusMention) {
    // we want nearby descriptors for pronouns
    if (focusMention.isPronoun()) {
      final Ordering<Spanning> descOrdering =
          // prefer the closest mention to the focusMention by sentence distance
          Spannings.InCloserSentenceTo(focusMention)
              // break ties by the earlier mention, then the longer mention
              .compound(Spannings.EarliestThenLongest);

      // restrict our options to DESC mentions
      // we are assured at the call site of this method that this maximum
      // exists
      return RepresentativeMention.from(e,
          descOrdering.max(FluentIterable.from(e).filter(OfType(Mention.Type.DESC))));
    } else {
      // fall back to default behavior
      return DefaultRepresentativeMentionFinder.get().representativeMentionForEntity(e);
    }
  }
}
