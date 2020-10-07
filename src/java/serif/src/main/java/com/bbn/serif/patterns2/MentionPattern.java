package com.bbn.serif.patterns2;

import com.bbn.bue.common.SExpression;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;

import org.immutables.value.Value;

/**
 * Pattern to match a Serif {@link Mention} or a full sentence containing a matching
 * {@link Mention}.
 *
 * {@code MentionPatterns} may specify the following criteria on
 * {@link com.bbn.serif.theories.Mentions}.
 *
 * <ul>
 *
 * <li>The mention must have a specific entity type, entity subtype, or entity label. If more than
 * one of these types of constraints is specified, a mention may match any of them. For instance,
 * if a pattern specification contains both {@code (acetype PER)} and {@code (entitylabel AGENT1)},
 * a mention may be either a {@code PER} or an {@code AGENT1} to match.
 *
 * <pre>
 *   (acetype PER ORG GPE)
 *   (acesubtype GPE.Nation GPE.Continent)
 *   (entitylabel AGENT1 AGENT1_EMPLOYEE)
 *   </pre>
 *
 *   </li>
 *
 *   <li>The mention must not have any of the specified entity labels. (Note: There is currently
 *   no way to block by entity type but this functionality could be added if necessary.
 *
 *   <pre>
 *     (block X Y Z)
 *     </pre>
 *
 *     </li>
 *
 * <li>The mention must have {@link Mention.Type} X, Y or Z. You must use the appropriate
 * four-letter lowercase identifier for the mention types you specify: {@code name}, {@code desc}
 * (descriptor), {@code pron} (pronoun), {@code part} (partitive), {@code appo} (appositive), or
 * {@code list}.
 *
 * <pre>
 *   (mentiontype X Y Z)
 *   </pre>
 *
 *   </li>
 *
 *   <li>The mention must have one of the specified headwords.
 *
 *   <pre>
 *     (headword teacher professor sensei)
 *     </pre>
 *
 *     </li>
 *
 *     <li>The mention must not have any of the specified headwords.
 *
 *     <pre>(block_headword teacher professor sensei)</pre>
 *
 *     </li>
 *
 *     <li>The mention must match a specified regular expression.
 *
 *     <pre>(regex REGEX)</pre>
 *
 *     </li>
 *
 *     <li>The mention’s head must match a specified regular expression.
 *     <pre>
 *       (head-regex REGEX)
 *       </pre>
 *       </li>
 *
 *       <li>The mention must be part of an entity with a name (somewhere in the document). So, a
 *       mention like “the prince” will only pass this constraint if it is co-referent with a name
 *       mention like “Prince Norodom Sihanouk” (somewhere else in the document).
 *
 *       <pre>(min-entitylevel NAME)</pre>
 *
 *       </li>
 *
 *       <li>The mention must be part of an entity with a name or a descriptor (somewhere in the
 *       document).  Here, “the prince” will pass this constraint regardless of whether the
 *       prince’s name is mentioned anywhere in the document, but a mention of “he” will not pass
 *       unless it is co-referent with a name or descriptor.
 *
 *       <pre>(min-entitylevel DESC)</pre>
 *
 *       </li>
 *
 *       <li>The mention must be part of a specific (or, conversely, a generic) entity. These two
 *       concepts are opposites. Very broadly, a generic mention is one that does not refer to a
 *       specific real-world entity but rather a generic type of entity, e.g. “skiers” in “skiers
 *       like the snow”. Serif does relatively poorly at specific/generic detection, however, so
 *       the use of this constraint may not be reliable.
 *
 *       <pre>
 *         SPECIFIC
 *         GENERIC
 *         </pre>
 *         </li>
 *
 * <li>The mention must be an appositive — note: this can also be captured by
 * <pre>(mentiontype appo)</pre>:
 *
 * <pre>APPOSITIVE</pre>
 *
 * </li>
 *
 * <li>The mention must be the child of an appositive entity.
 *
 * <pre>APPOSITIVE_CHILD</pre>
 *
 * </li>
 *
 * <li>The mention must be the child of an appositive entity that has at least one named child.
 * <pre>NAMED_APPOSITIVE_CHILD</pre>
 *
 * </li>
 *
 * </ul>
 *
 * <p>The {@code acetype}, {@code acesubtype}, and {@code entitylabel} constraints are treated as
 * a set. If one of them tests true, they are all considered to have tested true.</p>
 *
 * <p>For historical reasons, entity labels, entity types, and {@code }min-entity-level} are
 * specified using uppercase letters. Mention type constraints must use lowercase letters.</p>
 *
 * <p>Please note the very important difference between {@code min-entity-level} and
 * {@code mentiontype}. On the one hand, {@code (mentiontype desc)} will only allow the pattern to
 * fire on actual descriptor mentions. On the other hand, {@code (min-entity-level DESC)} will
 * allow the pattern to fire on any mention that is co-referent to a descriptor or any
 * higher-ranking mention type (i.e. a name). Also, note that because mentions not of a known
 * entity type (e.g. “the job” or “nuclear threats”) are not entities, they have no entity level,
 * and this constraint will always fail for them.</p>
 *
 * <p>Regex specifications may seem strange due to the repetition of the keyword {@code regex}, but
 * this is actually correct. For instance, {@code (mention (regex (regex (re ...)))}.</p>
 *
 * Examples:
 * <ul>
 *
 * <li><pre>(mention (acetype PER))</pre>: matches any {@code PER} mention</li>
 *
 * <li><pre>(mention (headword teacher))</pre>: matches any mention whose headword is teacher,
 * e.g. “the teacher”, “my teacher”, “the teacher who I liked the best”</li>
 *
 * <li><pre>(mention (acetype PER) (mentiontype name))</pre>: matches any {@code PER} name mention
 * </li>
 *
 * <li><pre>(mention (acetype PER) (min-entitylevel NAME))</pre>: matches any {@code PER} mention
 * that is co-referent with a name somewhere in the document</li>
 *
 * <li><pre>(mention (entitylabel AGENT1))</pre>:  matches any {@code AGENT1} mention</li>
 *
 * <li><pre>(mention (entitylabel AGENT1) (acesubtype GPE.Nation))</pre>: matches any {@code AGENT1}
 * mention or {@code GPE.Nation} mention</li>
 *
 * <li><pre>(mention (block AGENT1) (acetype GPE))</pre>: matches any {@code GPE} that is not an
 * {@code AGENT1} mention</li>
 *
 * <li><pre>(mention GENERIC (acetype GPE))</pre>: matches any generic {@code GPE}</li>
 *
 * <li><pre>(mention (acetype PER) (regex (regex (re (text (string “many”)))))</pre>: matches any
 * {@code PER} mention that contains the word “many”</li>
 *
 * </ul>
 *
 * Warning: this class is largely untested. See issue #333.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@Beta
@TextGroupImmutable
@Value.Immutable
@Value.Enclosing
public abstract class MentionPattern implements com.bbn.serif.patterns2.Pattern,
  MentionMatchingPattern  {
  //public abstract ImmutableSet<ComparisonConstraint> comparisonConstraints();
  public abstract ImmutableSet<EntityType> entityTypes();
  public abstract ImmutableSet<EntitySubtype> entitySubtypes();
  public abstract ImmutableSet<EntityType> blockedEntityTypes();
  public abstract ImmutableSet<Mention.Type> mentionTypes();
  public abstract ImmutableSet<Symbol> headwords();
  public abstract ImmutableSet<java.util.regex.Pattern> headwordPrefixes();
  public abstract ImmutableSet<Symbol> blockedHeadwords();
  public abstract ImmutableSet<java.util.regex.Pattern> blockedHeadwordPrefixes();
  public abstract ImmutableSet<Symbol> entityLabels();
  public abstract ImmutableSet<Symbol> blockingEntityLabels();
  public abstract Optional<RegexConstraint> regexConstraint();

  @Value.Default
  public boolean requiresName() {
    return false;
  }

  @Value.Default
  public boolean requiresNameOrDesc() {
    return false;
  }

  @Value.Default
  public boolean mustBeSpecific() {
    return false;
  }

  @Value.Default
  public boolean mustBeGeneric() {
    return false;
  }

  @Value.Default
  public boolean mustBeAppositive() {
    return false;
  }

  @Value.Default
  public boolean mustBeAppositiveChild() {
    return false;
  }

  @Value.Default
  public boolean mustBeNamedAppositiveChild() {
    return false;
  }

  @Value.Default
  public boolean blockFallThrough() {
    return false;
  }

  @Value.Immutable
  @TextGroupImmutable
  public static abstract class RegexConstraint {
    public abstract RegexPattern regexPattern();

    @Value.Default
    public boolean headRegex() {
      return false;
    }

    PatternReturns matches(DocTheory dt, SentenceTheory st, Mention m, PatternMatchState matchState) {
      if (headRegex()) {
        return regexPattern().matchesMentionHead(dt, st, m, matchState);
      } else {
        return regexPattern().match(dt, st, m, matchState, true);
      }
    }

    public static class Builder extends ImmutableMentionPattern.RegexConstraint.Builder {}
  }

  @Override
  public PatternReturns match(DocTheory dt, SentenceTheory st, Mention m,
      PatternMatchState matchState, boolean fallThroughChildren) {
    final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, m);
    if (cachedMatch.isPresent()) {
      return cachedMatch.get();
    }

    // to be even considered for a match, the pattern must match on either
    // entity type, subtype, or entity label
    final boolean matchesInTypeSubtypeOrEntityLabel =
        entityTypes().contains(m.entityType())
            || entitySubtypes().contains(m.entitySubtype())
            || matchesInEntityLabel(dt, m, matchState);

    final boolean matches = matchesInTypeSubtypeOrEntityLabel
        && passesMentionTypeConstraints(m)
        && passesAppositiveConstraints(m)
        && passesEntityConstraints(m.entity(dt))
        && !isExplicitlyBlocked(dt, m, matchState)
        && passesHeadWordConstraints(m)
        && matchesRegexConstraint(dt, st, m, matchState);

    if (matches) {
      final PatternMatch match = MentionPatternMatch.of(this, dt, st, m);
      return matchState.registerPatternMatch(this, m, match);
      // TODO: do we need to include regex pattern features as well? Issue #335
    } else {
      // TODO handle lists, parts, appositives, etc. Issue #334
      return matchState.registerUnmatched(this, m);
    }
  }


  public static class Builder extends ImmutableMentionPattern.Builder {
    public Builder addHeadwordsAccountingForWildcards(Symbol headword) {
      if (headword.asString().endsWith("*")) {
        addHeadwordPrefixes(java.util.regex.Pattern.compile(
            headword.asString().substring(0, headword.asString().length()-1) + ".*"));
      } else {
        addHeadwords(headword);
      }
      return this;
    }

    public Builder addBlockingHeadwordsAccountingForWildcards(Symbol headword) {
      if (headword.asString().endsWith("*")) {
        addBlockedHeadwordPrefixes(java.util.regex.Pattern.compile(
            headword.asString().substring(0, headword.asString().length()-1) + ".*"));
      } else {
        addBlockedHeadwords(headword);
      }
      return this;
    }
  }

  @Override
  public SExpression toSexpression() {
    // TODO: Issue #336
    throw new UnsupportedOperationException("Conversion to S-expression needs to be implemented");
    /*final SExpression.Builder ret = new SExpression.Builder();
    ret.addChildren(SExpression.of("mention"));
    ret.addChildren(SExpression.of("todo fill me in"));
    return ret.build();*/
  }


  private boolean passesMentionTypeConstraints(final Mention m) {
    return mentionTypes().isEmpty()
          || mentionTypes().contains(m.mentionType());
  }

  private boolean passesAppositiveConstraints(final Mention m) {
    final boolean passesAppositive = !mustBeAppositive() || Mention.Type.APPO.equals(m.mentionType());
    final boolean isAppositiveChild =
        m.parent().isPresent() && Mention.Type.APPO.equals(m.parent().get().mentionType());
    final boolean passesAppositiveChild = !mustBeAppositiveChild() || isAppositiveChild;

    final boolean isNamedAppositiveChild =
        isAppositiveChild && m.parent().get().hasNamedChild();
    final boolean passesNamedAppositiveChild = !mustBeNamedAppositiveChild() || isNamedAppositiveChild;

    return passesAppositive && passesAppositiveChild && passesNamedAppositiveChild;
  }

  private boolean passesEntityConstraints(final Optional<Entity> e) {
    return (!requiresName() || (e.isPresent() && e.get().hasNameMention()))
        && (!requiresNameOrDesc() || (e.isPresent() && e.get().hasNameOrDescMention()))
        && (!mustBeGeneric() || (e.isPresent() && e.get().isGeneric()))
        && (!mustBeSpecific() || (e.isPresent() && !e.get().isGeneric()));
  }

  private boolean matchesInEntityLabel(DocTheory dt, Mention m, PatternMatchState matchState) {
    final Optional<Entity> entity = m.entity(dt);
    if (entity.isPresent())
      for (final Symbol labelForEntity : matchState.labelsForEntity(entity.get())) {
        if (entityLabels().contains(labelForEntity)) {
          return true;
        }
      }
    return false;
  }

  private boolean isExplicitlyBlocked(final DocTheory dt, final Mention mention,
      final PatternMatchState matchState) {
    if (blockedEntityTypes().contains(mention.entityType())) {
      return true;
    }

    if (hasBlockedEntityLabel(dt, mention, matchState)) {
      return true;
    }

    // TODO: does this do anything to handle casing?
    final Symbol headWord = mention.node().headWord();
    if (blockedHeadwords().contains(headWord)) {
      return true;
    }

    // TODO: does this do anything to handle casing?
    for (final java.util.regex.Pattern blockedHwPrefix : blockedHeadwordPrefixes()) {
      if (blockedHwPrefix.matcher(headWord.asString()).matches()) {
        return true;
      }
    }

    return false;
  }

  private boolean passesHeadWordConstraints(Mention m) {
    final boolean passesExactMatch = headwords().isEmpty() || headwords().contains(m.node().headWord());
    boolean ret = headwordPrefixes().isEmpty() || passesExactMatch;
    for (final java.util.regex.Pattern headwordPrefix : headwordPrefixes()) {
      ret = ret || headwordPrefix.matcher(m.node().headWord().asString()).matches();
    }
    return ret;
  }

  private boolean matchesRegexConstraint(final DocTheory dt, final SentenceTheory st,
      final Mention m, final PatternMatchState matchState) {
    return !regexConstraint().isPresent() ||
        regexConstraint().get().matches(dt, st, m, matchState).matched();
  }


  // TODO: does this do anything to handle casing? Issue #337
  private boolean hasBlockedEntityLabel(final DocTheory dt, final Mention m,
      final PatternMatchState matchState) {
    final Optional<Entity> entity = m.entity(dt);
    if (entity.isPresent())
      for (final Symbol labelForEntity : matchState.labelsForEntity(entity.get())) {
        if (blockingEntityLabels().contains(labelForEntity)) {
          return true;
        }
      }
    return false;
  }

}
