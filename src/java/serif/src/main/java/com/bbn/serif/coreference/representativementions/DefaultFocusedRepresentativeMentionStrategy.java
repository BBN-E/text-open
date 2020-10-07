package com.bbn.serif.coreference.representativementions;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The default representative mention strategy where we use a specified mention as an anchor to bias
 * our results.  This class will be updated from time to time as we subjectively improve our
 * algorithm. The current approach is as follows: <p><ul> <li> If there is a name in the entity,
 * fall back on {link DefaultEntityBestMentionFinder}.</li> <li> If there is no descriptor in the
 * entity, return the focus mention.</li> <li> Otherwise (if there is a descriptor), it will return
 * the results from DescriptionFocusedRepresentativeMentionStrategy (see comment there).</li> </ul></p>
 *
 * @author rgabbard
 */
public enum DefaultFocusedRepresentativeMentionStrategy
    implements FocusedRepresentativeMentionStrategy {
  // why an enum? It makes enforcing singleton-ness easy.
  INSTANCE {
    @Override
    public Entity.RepresentativeMention representativeMentionForEntity(final Entity e,
        final Mention focusMention) {
      checkArgument(e.containsMention(focusMention));
      if (e.hasNameMention()) {
        return DefaultRepresentativeMentionFinder.get().representativeMentionForEntity(e);
      } else if (e.hasDescMention()) {
        return DescriptionFocusedRepresentativeMentionStrategy.get().representativeMentionForEntity(e, focusMention);
      } else {
        return Entity.RepresentativeMention.from(e, focusMention);
      }
    }
  };

  public static FocusedRepresentativeMentionStrategy get() {
    return INSTANCE;
  }
}
