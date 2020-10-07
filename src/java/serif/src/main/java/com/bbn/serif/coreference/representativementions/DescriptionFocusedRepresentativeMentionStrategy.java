package com.bbn.serif.coreference.representativementions;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.Spannings;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Ordering;

import static com.bbn.serif.theories.Mention.OfType;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.filter;

/**
 * The description focused representative mention strategy uses a specified mention as an
 * anchor to bias our results ("focused") and prefers to return a description if possible
 * ("description"). If there is a descriptor in the entity, it prefers the closest coreferent
 * descriptor to the focus mention by token distance, regardless of direction. This could be the
 * focus mention itself.  In case of ties, the earlier mention is preferred. If there is still a
 * tie, the longer mention is preferred. If there is no descriptor, it just returns the original
 * focus mention.
 *
 * @author eboschee
 */
public enum DescriptionFocusedRepresentativeMentionStrategy implements
    FocusedRepresentativeMentionStrategy {

  // why an enum? It makes enforcing singleton-ness easy.
  INSTANCE {
    @Override
    public Entity.RepresentativeMention representativeMentionForEntity(final Entity e,
    final Mention focusMention) {
      checkArgument(e.containsMention(focusMention));
      if (e.hasDescMention()) {
        return bestDescMention(e, focusMention);
      } else {
        // Must return a result, so we just return the mention that was passed in
        return Entity.RepresentativeMention.from(e, focusMention);
      }
    }

  private Entity.RepresentativeMention bestDescMention(final Entity e,
      final Mention focusMention) {
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
      return Entity.RepresentativeMention.from(e,
          descOrdering.max(FluentIterable.from(e).filter(OfType(Mention.Type.DESC))));
    } else {
      // There must be at least one desc mention or this function would not have been called
      // Note that this does NOT take the focusMention into consideration;
      //  it just returns the earliest and then longest descriptor.
      // This may not be what one wants, descriptor coreference being dicey, but it
      //  replicates previously existing behavior.
      return Entity.RepresentativeMention.from(e,
          Spannings.EarliestThenLongest.max(
              filter(e, OfType(Mention.Type.DESC))));
    }
  }
};

public static DescriptionFocusedRepresentativeMentionStrategy get() {
    return INSTANCE;
    }

}
