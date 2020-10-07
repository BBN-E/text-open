package com.bbn.serif.patterns2;

import com.bbn.bue.common.CodepointMatcher;
import com.bbn.bue.common.SExpression;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.annotations.MoveToBUECommon;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.ValueMention;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * RegexPatterns match sentences, arguments, or mentions, using regular expressions together with
 * {@link MentionPattern}s and {@link ValueMentionPattern}s.
 *
 * <p>The most important piece is the {@code re} constraint, an ordered list of
 * {@link MentionPattern}s, {@link ValueMentionPattern}s, and {@link TextPattern}s.
 * Spaces are inserted between each of these when the regular expression to be matched against
 * space-separated tokenized text unless the {@code DONT_ADD_SPACES} constraint
 * is specified. If the constraint is added, the user must be careful to add spaces before and
 * after any {@link MentionPattern}s and {@link ValueMentionPattern}s to have the possibility of
 * a match.</p>
 *
 * <p>Note: RegexPatterns cannot start or end matching in a middle of a word. For example if the
 * RegexPattern starts with the word “the”, it will not start matching in the middle of the
 * word “gather”.</p>
 *
 * <p>The list of subpatterns will amost always include one or more text elements, which may be
 * defined elsewhere and referenced using shortcuts, and which may obviously have their own return
 * values. (This allows you to easily extract smaller portions of regular expressions.)</p>
 *
 * <p>When trying to “match” a MentionPattern, the system will consider both the full mention extent
 * and the head mention extent to be valid matches.  So, imagine the sentence “The president of
 * Cuba said that…”. The following pattern will match using the full mention extent:</p>
 *
 * <pre>
 *   (regex (re (mention (acetype PER)) (text (string “said”))))
 *   </pre>
 *
 * <p>matches “The president of Cuba said.” The following pattern will match using the head mention
 * extent:</p>
 *
 * <pre>
 * (regex (re (mention (acetype PER)) (text (string “of Cuba”))))
 * </pre>
 *
 * <p>matches “The president of Cuba said.” The second type of matching behavior can be turned off
 * by including the constraint {@code DONT_ALLOW_HEADS}.</p>
 *
 * <p>RegexPatterns can specify any of the following constraints:

 * <ul>
 *
 * <li>{@code DONT_ALLOW_HEADS}: does not try to match the heads of the
 * sub-{@code MentionPattern}s.</li>
 *
 * <li>{@code MATCH_FULL_EXTENT}: will only match if the entire text of the target element matches
 * the {@code RegexPattern}.</li>
 *
 * <li>{@code DONT_ADD_SPACES}: spaces will not be automatically added between the subpatterns of
 * the {@code RegexPattern} when they are concatenated.
 *
 * </ul>
 *
 * </p>
 *
 * <p>When specifying a {@code RegexPattern}, the {@code DONT_ALLOW_HEADS} and
 * {@code MATCH_FULL_EXTENT} must occur before the {@code re} constraint. However, in many use
 * cases these constraints will never be applied, and most patterns will be of the form
 * <pre>(regex (re ...))</pre>.</p>
 *
 * <ul>Examples:
 *
 * <li><pre>
 *   (regex (re (mention (acetype PER))
 *              (text (string ".{0,10} born( in| on)?"))
 *              (text (string "\d\d\d\d"))))
 *              </pre>
 *
 * matches any person closely followed by the word born, possibly the word “in” or “on”, and
 * then a four digit number</li>
 *
 * <li>
 *   <pre>
 *     (regex DONT_ADD_SPACES
 *     (re (text (string "\d+[ \n]*|\d{1,3}(,\d{3})*[ \n]* |(one|two|three|four|five|six|seven|eight
 * |nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty
 * |thirty|forty|fifty|sixty|seventy|eighty|ninety| |-)+[ \n]*"))
 * (text (string "(hundred|thousand|million|billion|trillion)?"))))
 * </pre>
 * matches many numbers (ideally would be expanded to allow for additional repetition of
 * the elements)

 * </li>
 * </ul>
 */
@Beta
@TextGroupImmutable
@Value.Immutable
@Value.Enclosing
public abstract class RegexPattern implements MentionMatchingPattern, SentenceMatchingPattern {
  private static final Logger log = LoggerFactory.getLogger(RegexPattern.class);

  public abstract ImmutableList<Pattern> subpatterns();

  @Value.Default
  public boolean topMentionsOnly() {
    return false;
  }

  @Value.Default
  public boolean matchWholeExtent(){
    return false;
  }

  @Value.Default
  public boolean allowHeads(){
    return true;
  }

  @Value.Default
  public boolean addSpaces(){
    return true;
  }

  public static class Builder extends ImmutableRegexPattern.Builder {}

  @Override
  public Optional<Symbol> shortcut() {
    return null;
  }

  @Override
  public Optional<Double> score() {
    return null;
  }

  @Override
  public Optional<ScoreGroup> scoreGroup() {
    return null;
  }

  @Override
  public Optional<ScoreFunction> scoreFunction() {
    return null;
  }

  @Override
  public Optional<Symbol> patternLabel() {
    return null;
  }

  @Override
  public SExpression toSexpression() {
    // TODO: #336
    throw new UnsupportedOperationException();
  }

  private static final String SUBPATTERN_MATCH_STRING = "!@SUBPAT_MATCH@!";
  private static final java.util.regex.Pattern APPARENT_START_GROUP =
      java.util.regex.Pattern.compile("\\(");
  private static final java.util.regex.Pattern ESCAPED_PAREN =
      java.util.regex.Pattern.compile("\\\\\\(");

  /**
   * This is the regular expression which will be matched against the sentence text with
   * sub-pattern matches replaced by a special string
   */
  @Value.Derived
  protected InternalRegexInfo regexPattern() {
    final StringBuilder regex = new StringBuilder();
    final InternalRegexInfo.Builder ret = new InternalRegexInfo.Builder();

    if (matchWholeExtent()) {
      regex.append("^");
    }
    // when we construct the string to match against below, we always begin it with a space
    // to avoid matching in the middle of words
    regex.append(" ");
    int nextMatchingGroup = 0;
    for (final Pattern subPattern : subpatterns()) {
      if (subPattern instanceof TextPattern) {
        final TextPattern textSubPattern = (TextPattern)subPattern;
        regex.append("(");
        // TODO: check if textPattern's getText does anything special on the C++ side
        // issue #339
        regex.append(textSubPattern.text());
        regex.append(")");
        if (addSpaces()) {
          regex.append(" ");
        }
        ret.putSubPatternToRegexGroup(subPattern, nextMatchingGroup++);
        // the text patterns could have their own escaped groups which we need to count
        nextMatchingGroup += countMatches(textSubPattern.text(),APPARENT_START_GROUP)
            // don't count literal (escaped) parentheses which don't form matching groups, though
          - countMatches(textSubPattern.text(), ESCAPED_PAREN);
      } else {
        regex.append("(" + SUBPATTERN_MATCH_STRING + ")");
        if (addSpaces()) {
          regex.append(" ");
        }
        ret.putSubPatternToRegexGroup(subPattern, nextMatchingGroup++);
      }
    }

    if (!addSpaces()) {
      // Even if we aren't adding spaces, we need to put a space in before the end, since
      // the sentence will have a space at the end. And if matchWholeExtent is on, we
      // want to make sure the spaces match up.
      regex.append(" ");
    }
    if (matchWholeExtent()) {
      regex.append('$');
    } else {
      // remove trailing space, and add "look ahead"
      // Make sure a space exists after the regex match,
      // but don't consume it. This makes it so we can find
      // two matches back to back.
      regex.deleteCharAt(regex.length()-1);
      regex.append("(?= )");
    }
    ret.regexPattern(java.util.regex.Pattern.compile(regex.toString()));
    return ret.build();
  }

  @MoveToBUECommon
  private static int countMatches(String s, java.util.regex.Pattern regex) {
    int matches = 0;
    int curPos = 0;
    final Matcher matcher = regex.matcher(s);
    while (matcher.find(curPos)) {
      ++matches;
      curPos = matcher.start() + 1;
    }
    return matches;
  }

  @TextGroupImmutable
  @Value.Immutable
  static abstract class InternalRegexInfo {
    public abstract java.util.regex.Pattern regexPattern();

    /**
     * {@link #regexPattern()} will have matching groups corresponding to each sub-pattern,
     * but the user-written regexes may also contain matching groups, so we need to
     * track the correspondence
     */
    public abstract ImmutableMap<Pattern, Integer> subPatternToRegexGroup();

    public static class Builder extends ImmutableRegexPattern.InternalRegexInfo.Builder {}
  }

  @Value.Check
  protected void check() {
    checkArgument(!subpatterns().isEmpty());
    for (final Pattern subpattern : subpatterns()) {
      checkArgument(subpattern instanceof TextPattern
          || subpattern instanceof MentionPattern
          || subpattern instanceof ValueMentionPattern);
    }
  }

  @Override
  public PatternReturns match(final DocTheory dt, final SentenceTheory st,
      PatternMatchState matchState) {
    final ImmutableSet<PatternReturns> matched = matchesTokenSpan(dt, st, st.span(), matchState);
    if (!matched.isEmpty()) {
      final PatternMatch match = SentencePatternMatch.of(this, dt, st);
      // TODO: do we need to include regex pattern features as well? issue #335
      return matchState.registerPatternMatch(this, st, match);
    } else {
      return matchState.registerUnmatched(this, st);
    }
  }


  @Override
  public PatternReturns match(final DocTheory dt, final SentenceTheory st, final Mention m,
      final PatternMatchState matchState, final boolean fallThroughChildren) {
    // TODO: should we allow fallThroughChildren? #340
    final ImmutableSet<PatternReturns> matched = matchesTokenSpan(dt, st, m.span(), matchState);
    if (!matched.isEmpty()) {
      final PatternReturns.Builder ret =
          new PatternReturns.Builder().from(matchState.registerPatternMatch(this, m,
              MentionPatternMatch.of(this, dt, st, m)));
      // include matches from within regex
      for (final PatternReturns patternReturn : matched) {
        ret.addAllMatches(patternReturn.matches());
      }
      return ret.build();
    } else {
      return matchState.registerUnmatched(this, m);
    }
  }

  public PatternReturns matchesMentionHead(final DocTheory dt, final SentenceTheory st, final Mention m,
      final PatternMatchState matchState) {
    final ImmutableSet<PatternReturns>
        matched = matchesTokenSpan(dt, st, m.node().head().span(), matchState);
    if (!matched.isEmpty()) {
      final PatternMatch match = MentionPatternMatch.of(this, dt, st, m);
      // TODO: do we need to include regex pattern features as well? #335
      return matchState.registerPatternMatch(this, m, match);
    } else {
      return matchState.registerUnmatched(this, m);
    }
  }

  private ImmutableSet<PatternReturns> matchesTokenSpan(final DocTheory dt, final SentenceTheory st,
      final TokenSequence.Span searchSpan, final PatternMatchState matchState) {

    final ImmutableSetMultimap.Builder<Pattern, PatternMatch> subPatternMatches =
        ImmutableSetMultimap.builder();

    if (!gatherSubpatternMatches(dt, st, searchSpan, matchState, subPatternMatches)) {
      return ImmutableSet.of();
    }

    final ImmutableSet.Builder<PatternReturns> ret = ImmutableSet.builder();
    // this is a direct port of the somewhat wacky implementation in CSerif. We could do better
    // with some cleverness
    int innerLoopCount = 0;
    for (final RegexSentence regexSent : regexSentences(st, searchSpan, subPatternMatches.build())) {
      if (innerLoopCount++ > 1000) {
        log.warn("Exceeded inner loop count matching regexp pattern {}", this);
        break;
      }

      final Matcher matcher = regexPattern().regexPattern().matcher(regexSent.codeUnits());
      while (matcher.find()) {
        final String fullMatch = matcher.group(0);
        if (fullMatch.isEmpty() || CodepointMatcher.whitespace().matchesAllOf(fullMatch)) {
          // match was the empty string or all whitespace
          continue;
        }
        final int startTokIdx = regexSent.startTokenForCodeUnitOffset(matcher.start(0));
        final int endTokIdx = regexSent.endTokenForCodeUnitOffset(matcher.end(0) - 1);

        ret.add(matchState.registerPatternMatch(this, searchSpan,
            TokenSpanPatternMatch.of(dt, st, st.tokenSequence().span(startTokIdx, endTokIdx))));
        // addID?
        // set pattern match score
        // collect sub-pattern matches
      }
    }

    return ret.build();
  }

  // we check all cases of this in our preconditions
  @SuppressWarnings("OptionalGetWithoutIsPresent")
  ImmutableList<RegexSentence> regexSentences(final SentenceTheory st,
      final TokenSequence.Span searchSpan,
      final SetMultimap<Pattern, PatternMatch> subPatternMatches) {
    // simplifies code below by letting us assume the token sequence is non-empty
    if (st.tokenSequence().isEmpty()) {
      return ImmutableList.of();
    }

    final List<Set<PatternMatch>> subPatternMatchesAsSets = new ArrayList<>();
    for (final Collection<PatternMatch> matchesForSubpattern : subPatternMatches.asMap()
        .values()) {
      // sanity check our preconditions that all pattern matches have Spannings and are
      // within the search span.
      for (final PatternMatch matchForSubPattern : matchesForSubpattern) {
        checkArgument(matchForSubPattern.spanning().isPresent());
        checkArgument(searchSpan.contains(matchForSubPattern.spanning().get().span()));
      }
      // build the data structure we need as input to Sets.cartesianProduct
      subPatternMatchesAsSets.add(ImmutableSet.copyOf(matchesForSubpattern));
    }

    // get all possible combinations of sub-pattern matches
    final Set<List<PatternMatch>> subPatternMatchCombinations =
        Sets.cartesianProduct(subPatternMatchesAsSets);

    final ImmutableList.Builder<RegexSentence> ret = ImmutableList.builder();

    for (final List<PatternMatch> subPatternMatchCombination : subPatternMatchCombinations) {
      final RegexSentence.Builder regexSentence = RegexSentence.builder();

      int curTokIdx = searchSpan.startIndex();

      // We put a space at the beginning of a regex pattern, mapped to the first token,
      // so we don't start matching in the middle of a word.
      regexSentence.append(" ", curTokIdx, curTokIdx);

      for (int i = 0; i < subPatternMatchCombination.size(); ++i) {
        final Optional<Spanning> curSubPatternMatch = subPatternMatchCombination.get(i).spanning();
        if (i + 1 < subPatternMatchCombination.size()) {
          final Optional<Spanning> nextSubPatternMatch =
              subPatternMatchCombination.get(i + 1).spanning();
          final boolean areCorrectlyOrdered =
              curSubPatternMatch.get().span().endsBefore(nextSubPatternMatch.get().span());
          if (!areCorrectlyOrdered) {
            continue;
          }
          // this should be guaranteed as a precondition
          checkState(curSubPatternMatch.isPresent() && nextSubPatternMatch.isPresent());
        }

        for (; curTokIdx < curSubPatternMatch.get().span().startIndex(); ++curTokIdx) {
          regexSentence.append(
              st.tokenSequence().token(curTokIdx).tokenizedText().utf16CodeUnits(),
              curTokIdx, curTokIdx);
          // we need a space after every token but the last, but we know this is not
          // the last token because there is a sub-pattern match after it
          regexSentence.append(" ", curTokIdx, curTokIdx);
        }

        // note the extra space to ensure all tokens and sub-pattern match strings
        // are space-separated
        regexSentence.append(SUBPATTERN_MATCH_STRING + " ",
            curSubPatternMatch.get().span().startIndex(),
            curSubPatternMatch.get().span().endIndex());

        // skip over the tokens which are part of the sub-pattern match
        curTokIdx = curSubPatternMatch.get().span().endIndex() + 1;
      }

      // mop up any trailing tokens after the last sub-pattern match (this will get all
      // tokens if there were no sub-patterns
      for (; curTokIdx <= searchSpan.endIndex(); ++curTokIdx) {
        regexSentence.append(st.tokenSequence().token(curTokIdx).tokenizedText().utf16CodeUnits(),
            curTokIdx, curTokIdx);
        final int followingSpaceTokIdx = (curTokIdx + 1 < searchSpan.endIndex()) ? (curTokIdx + 1) : curTokIdx;
        regexSentence.append(" ", curTokIdx, followingSpaceTokIdx);
      }
      ret.add(regexSentence.build());
    }

    return ret.build();
  }

  private boolean gatherSubpatternMatches(final DocTheory dt, final SentenceTheory st,
      final TokenSequence.Span tokenSpan, final PatternMatchState matchState,
      final ImmutableSetMultimap.Builder<Pattern, PatternMatch> subPatternMatches) {
    boolean ret = true;
    // first we try to match all complex sub-patterns
    for (final Pattern subpattern : subpatterns()) {
      if (subpattern instanceof TextPattern)  {
        // these will be handled separately
        continue;
      }
      boolean found = false;
      if (subpattern instanceof MentionMatchingPattern) {
        for (final Mention m : st.mentions()) {
          // no need to check out-of-bounds mentions
          if (tokenSpan.contains(m.span())) {
            // falling through sets in regexes is always okay
            final PatternReturns
                matches = ((MentionMatchingPattern) subpattern).match(dt, st, m, matchState, true);
            if (matches.matched()) {
              found = true;
              // below TODOs are issue #341
              // TODO: there is a check in the CSerif I don't understand here
              //if (!isTopLevel(m)) {
              //  continue;
              //}
              // TODO: figure out allow_heads behavior
              subPatternMatches.putAll(subpattern, matches.matches());
            }
          }
        }
      } else if (subpattern instanceof ValueMentionMatchingPattern) {
        final ValueMentionMatchingPattern vmPattern = ((ValueMentionMatchingPattern) subpattern);
        for (final ValueMention vm : st.valueMentions()) {
          applyValueMentionSubPattern(dt, st, vmPattern, vm, subPatternMatches, matchState);
        }
        for (final ValueMention docLevelVm : dt.valueMentions()) {
          applyValueMentionSubPattern(dt, st, vmPattern, docLevelVm,
              subPatternMatches, matchState);
        }
      }

      if (!found) {
        ret = false;
      }
    }
    return ret;
  }

  private boolean applyValueMentionSubPattern(DocTheory dt, SentenceTheory st,
      final ValueMentionMatchingPattern pattern, final ValueMention vm,
      final ImmutableSetMultimap.Builder<Pattern, PatternMatch> patternMatches,
      final PatternMatchState matchState) {
    final PatternReturns matches = pattern.match(dt, st, vm, matchState);
    patternMatches.putAll(pattern, matches.matches());
    return matches.matched();
  }

}


final class RegexSentence {
  private final String text;
  private final ImmutableList<Integer> codeUnitOffsetToStartTokenIdx;
  private final ImmutableList<Integer> codeUnitOffsetToEndTokenIdx;

  private RegexSentence(final String text,
      final List<Integer> codeUnitOffsetToStartTokenIdx,
      final List<Integer> codeUnitOffsetToEndTokenIdx) {
    checkArgument(text.length() == codeUnitOffsetToStartTokenIdx.size());
    checkArgument(text.length() == codeUnitOffsetToEndTokenIdx.size());
    this.text = checkNotNull(text);
    this.codeUnitOffsetToStartTokenIdx = ImmutableList.copyOf(codeUnitOffsetToStartTokenIdx);
    this.codeUnitOffsetToEndTokenIdx = ImmutableList.copyOf(codeUnitOffsetToEndTokenIdx);
  }

  public String codeUnits() {
    return text;
  }

  public int startTokenForCodeUnitOffset(final int codeUnitOffset) {
    return codeUnitOffsetToStartTokenIdx.get(codeUnitOffset);
  }

  public int endTokenForCodeUnitOffset(final int codeUnitOffset) {
    return codeUnitOffsetToEndTokenIdx.get(codeUnitOffset);
  }

  public static Builder builder () {
    return new Builder();
  }

  public static final class Builder {
    private final StringBuilder text = new StringBuilder();
    private final ImmutableList.Builder<Integer> codeUnitOffsetToStartTokenIdx =
        ImmutableList.builder();
    private final ImmutableList.Builder<Integer> codeUnitOffsetToEndTokenIdx =
        ImmutableList.builder();

    private Builder() {}

    Builder append(String codeUnits, int startToken, int endToken) {
      text.append(codeUnits);
      for (int i=0; i<codeUnits.length(); ++i) {
        codeUnitOffsetToStartTokenIdx.add(startToken);
        codeUnitOffsetToEndTokenIdx.add(endToken);
      }
      return this;
    }

    public RegexSentence build() {
      return new RegexSentence(text.toString(), codeUnitOffsetToStartTokenIdx.build(),
          codeUnitOffsetToEndTokenIdx.build());
    }
  }


}


