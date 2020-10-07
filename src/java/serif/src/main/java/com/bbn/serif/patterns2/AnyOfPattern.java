package com.bbn.serif.patterns2;

import com.bbn.bue.common.TextGroupImmutable;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A pattern which matches if any of its sub-patterns match on a single extraction unit.
 *
 * <p>Combination patterns ({@code AllOfPattern}, {@link AnyOfPattern}, {@link NoneOfPattern}
 * match over specific extraction units, namely {@link com.bbn.serif.theories.EventMention}s,
 * {@link com.bbn.serif.theories.RelationMention}s, {@link com.bbn.serif.theories.Proposition}s,
 * {@link com.bbn.serif.theories.Mention}s, or
 * {@link com.bbn.serif.theories.EventMention.Argument}s.
 * Any type of pattern can be a member, but all members must be of the same pattern type.
 * These may have any number of sub-patterns.
 *
 * Example:
 * <pre>
 *  (any-of (members (mention (acetype PER))
 *                   (mention (headword company troop unit))))
 *
 * selects mentions that are either of type {@code PER} or whose headword
 * is <i>company/troop/unit</i>
 *
 * <p>Pattern combinations can be particularly useful when using shortcuts. Even if a pattern
 * combination is not strictly necessary to express the desired functionality, it might provide a
 * more compact representation. For instance:
 * <pre>
 *   (vprop (shortcut PER_SUB_PROP)
 *          (args (argument (role <sub>) (mention (acetype PER)))))
 *   (vprop (shortcut VISIT) (predicate visit visited visits visiting))
 *   (all-of (members PER_SUB_PROP VISIT)
 *   </pre>
 *
 * selects propositions whose subject is a person and whose predicate is <i>visit</i>, etc.
 *
 * Note that while you can construct an {@code AllOfPattern}, you cannot currently execute one.
 * For that, see issue #331
 */
@Beta
@TextGroupImmutable
public abstract class AnyOfPattern implements Pattern {
  public abstract ImmutableSet<Pattern> patterns();

  @Value.Check
  protected void check() {
    checkArgument(!patterns().isEmpty());
  }
}
