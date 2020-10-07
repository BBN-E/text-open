package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.serif.types.ValueType;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

/**
 * Matches {@link com.bbn.serif.theories.ValueMention}s or a full sentence (if and only if that
 * sentence contains a matching {@link com.bbn.serif.theories.ValueMention}).
 *
 * <p>ValueMentionPatterns can specify any of the following constraints:
 *
 * <ul>
 *
 *   <li>The value mention must have a specific type.
 *
 *   <pre>
 *     (type EMAIL PHONE)
 *     </pre>
 *
 *     </li>
 *     <li>The value mention must be part of a specific date (i.e. one resolved to a specific
 *     TIMEX calendar period).
 *
 *     <pre>
 *       SPECIFIC-DATE
 *       </pre>
 *
 *       </li>
 *
 *       <li>The value must be a date within 30 days of the document date. This will cause
 *       the pattern to fail unless all of the following are true: the {@code PatternMatcher} knows
 *       the document date, the value date has year, month and day specified, the value date is
 *       before or identical to the document date, and the value date is within 30 days of the
 *       document date.
 *
 *       <pre>
 *         RECENT-DATE
 *         </pre>
 *
 *         </li>
 *
 *  <li>The value mention be in or out of the activity date range. Activity date ranges are only
 *  relevant for a query-based task like Distillation. If there is no activity date specified,
 *  these constraints are ignored.
 *
 *  <pre>
 *    (activity-date IN_RANGE)
 *    (activity-date OUT_OF_RANGE)
 *    </pre>
 *
 *    </li>
 *
 *    <li>The value mention must match a specified regular expression.
 *
 *    <pre>
 *      (regex REGEX)
 *      </pre>
 *
 *      </li>
 *      </ul></p>
 *
 * <p></p>Please note that SPECIFIC-DATE is not always reliable, especially in documents without
 * timestamps.</p>
 *
 * <ul>Examples:
 *
 * <li> {@code (value (type PHONE EMAIL URL))}: matches any phone, email, or url</li>
 *
 * <li>{@code (value (activity-date OUT_OF_RANGE))}: matches any date not in the activity date
 * range</li>
 *
 * <li>{@code (value SPECIFIC-DATE (activity-date IN_RANGE))}: matches any date in the activity
 * date range</li>
 *
 * </ul>
 */
@Beta
@TextGroupImmutable
public abstract class ValueMentionPattern  implements Pattern {
  public abstract ImmutableSet<ValueType> valueTypes();

  @Value.Default
  public boolean mustBeSpecificDate() {
    return false;
  }

  @Value.Default
  public boolean mustBeRecentDate() {
    return false;
  }

  @Value.Default
  public boolean mustBeFutureDate() {
    return false;
  }

  public abstract Optional<Pattern> regexPattern();

  private static final int RECENT_DAYS_CONSTRAINT = 30;
  public abstract Optional<DateStatus> activityDateStatus();

  public enum DateStatus {
    IN_RANGE, OUT_OF_RANGE, NOT_SPECIFIC, TOO_BROAD
  }
}
