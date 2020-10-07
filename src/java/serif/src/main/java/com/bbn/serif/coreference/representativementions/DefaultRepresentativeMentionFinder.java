package com.bbn.serif.coreference.representativementions;

import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Entity.RepresentativeMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Spannings;

import com.google.common.annotations.Beta;

import static com.bbn.serif.theories.Mention.OfType;
import static com.google.common.collect.Iterables.filter;

/**
 * The default representative mention finder.  This class will be updated from time to time as we
 * subjectively improve our algorithm. The current approach is as follows: <p><ul> <li> If there is
 * a name in the entity, return the longest name by AtomicStringLength.  In case of ties, return the
 * first mention according to the ordering in the entity.</li> <li> Otherwise, if there is a
 * descriptor in the entity, return the earliest one in the document. In case of ties, return the
 * longest.</li> <li> Otherwise, return the first mention in the entity.</li> </ul></p>
 *
 * @author rgabbard
 */
@Beta
public final class DefaultRepresentativeMentionFinder implements RepresentativeMentionFinder {

  private DefaultRepresentativeMentionFinder() {
  }

  private static final RepresentativeMentionFinder SINGLETON =
      new DefaultRepresentativeMentionFinder();

  public static RepresentativeMentionFinder get() {
    return SINGLETON;
  }

  @Override
  public RepresentativeMention representativeMentionForEntity(final Entity e) {
    if (e.hasNameMention()) {
      return bestNameMention(e);
    } else if (e.hasDescMention()) {
      return bestDescMention(e);
    } else {
      return RepresentativeMention.from(e, e.mention(0));
    }
  }

  /**
   * Returns the longest name mention, where length is that of the {@code atomicCasedTextString}.
   */
  private static RepresentativeMention bestNameMention(final Entity e) {
    // if possible, take a child-less mention
    //return PreferChildless.compound(
    // if possible, take a country mention
    final Mention ret = RepresentativeMentionUtils.PreferCountries
        // next, take the longest by atomic string length that we can
        .compound(Mention.ByAtomicStringLength).max(
            // among those mentions of type NAME
            filter(e, OfType(Mention.Type.NAME)));

    return RepresentativeMention.from(e, ret, ret.atomicHead().span());
  }

  private static RepresentativeMention bestDescMention(final Entity e) {
    // we are guaranteed at the call site that this maximum exists
    return RepresentativeMention.from(e,
        Spannings.EarliestThenLongest.max(
            filter(e, OfType(Mention.Type.DESC))));
  }


}
