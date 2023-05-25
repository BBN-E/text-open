package com.bbn.serif.patterns;

import com.bbn.serif.patterns.matching.EventMatchingPattern;
import com.bbn.serif.patterns.matching.MentionMatchingPattern;
import com.bbn.serif.patterns.matching.PatternMatch;
import com.bbn.serif.patterns.matching.PatternMatchState;
import com.bbn.serif.patterns.matching.PatternReturns;
import com.bbn.serif.patterns.matching.SentenceMatchingPattern;
import com.bbn.serif.patterns.matching.TokenSpanPatternMatch;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;

import com.google.common.base.Optional;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;


public final class RegexPattern extends LanguageVariantSwitchingPattern implements
        MentionMatchingPattern, EventMatchingPattern, SentenceMatchingPattern {

  private final List<Pattern> subpatterns;
  private final boolean topMentionsOnly;
  private final boolean allowHeads;
  private final boolean matchWholeExtent;
  private final boolean addSpaces;

  private java.util.regex.Pattern regexPattern;

  /**
   * getter method for subpatterns
   */
  public List<Pattern> getSubpatterns() {
    return this.subpatterns;
  }

  /**
   * getter method for topMentionsOnly
   */
  public boolean isTopMentionsOnly() {
    return this.topMentionsOnly;
  }

  /**
   * getter method for allowHeads
   */
  public boolean isAllowHeads() {
    return this.allowHeads;
  }

  /**
   * getter method for matchWholeExtent
   */
  public boolean isMatchWholeExtent() {
    return this.matchWholeExtent;
  }

  /**
   * getter method for addSpaces
   */
  public boolean isAddSpaces() {
    return this.addSpaces;
  }

  public RegexPattern(final Builder builder) {
    super(builder);
    this.subpatterns = builder.subpatterns;
    this.topMentionsOnly = builder.topMentionsOnly;
    this.allowHeads = builder.allowHeads;
    this.matchWholeExtent = builder.matchWholeExtent;
    this.addSpaces = builder.addSpaces;
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withSubpatterns(subpatterns);
    b.withTopMentionsOnly(topMentionsOnly);
    b.withAllowHeads(allowHeads);
    b.withMatchWholeExtent(matchWholeExtent);
    b.withAddSpaces(addSpaces);
    return b;
  }

  public static class Builder extends LanguageVariantSwitchingPattern.Builder {

    private List<Pattern> subpatterns;
    private boolean topMentionsOnly = false;
    private boolean allowHeads = true;
    private boolean matchWholeExtent = false;
    private boolean addSpaces = true;

    public Builder() {
    }

    @Override
    public RegexPattern build() {
      return new RegexPattern(this);
    }

    public Builder withSubpatterns(final List<Pattern> subpatterns) {
      this.subpatterns = subpatterns;
      return this;
    }

    public Builder withTopMentionsOnly(final boolean topMentionsOnly) {
      this.topMentionsOnly = topMentionsOnly;
      return this;
    }

    public Builder withAllowHeads(final boolean allowHeads) {
      this.allowHeads = allowHeads;
      return this;
    }

    public Builder withMatchWholeExtent(final boolean matchWholeExtent) {
      this.matchWholeExtent = matchWholeExtent;
      return this;
    }

    public Builder withAddSpaces(final boolean addSpaces) {
      this.addSpaces = addSpaces;
      return this;
    }
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Pattern p : subpatterns) {
      if (!first)
        sb.append(" ");
      first = false;
      sb.append(p.toPrettyString());
    }

    return sb.toString().replaceAll("\\s+", " ");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (addSpaces ? 1231 : 1237);
    result = prime * result + (allowHeads ? 1231 : 1237);
    result = prime * result + (matchWholeExtent ? 1231 : 1237);
    result = prime * result
        + ((subpatterns == null) ? 0 : subpatterns.hashCode());
    result = prime * result + (topMentionsOnly ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RegexPattern other = (RegexPattern) obj;
    if (addSpaces != other.addSpaces) {
      return false;
    }
    if (allowHeads != other.allowHeads) {
      return false;
    }
    if (matchWholeExtent != other.matchWholeExtent) {
      return false;
    }
    if (subpatterns == null) {
      if (other.subpatterns != null) {
        return false;
      }
    } else if (!subpatterns.equals(other.subpatterns)) {
      return false;
    }
    if (topMentionsOnly != other.topMentionsOnly) {
      return false;
    }
    return true;
  }

  @Override
  public PatternReturns match(DocTheory dt, SentenceTheory st,
                              PatternMatchState matchState, boolean fallThroughChildren) {
//    System.out.println("Matching regex pattern against sentence: " + st.tokenSequence().text());

    final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, st);
    if (cachedMatch.isPresent()) {
      return cachedMatch.get();
    }

    int startToken = st.tokenSpan().startTokenIndexInclusive();
    int endToken = st.tokenSpan().endTokenIndexInclusive();

    final Optional<PatternMatch> match = matchImpl(dt, st, startToken, endToken);

    if (match.isPresent())
      return matchState.registerPatternMatch(this, st, match.get());
    else
      return matchState.registerUnmatched(this, st);
  }

  public PatternReturns match(DocTheory dt, SentenceTheory st, Mention m,
                              PatternMatchState matchState, boolean fallThroughChildren) {

    //System.out.println("Matching regex pattern against mention: " + m.tokenSpan().originalText().toString());

    final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, m);
    if (cachedMatch.isPresent()) {
      return cachedMatch.get();
    }

    int startToken = m.tokenSpan().startTokenIndexInclusive();
    int endToken = m.tokenSpan().endTokenIndexInclusive();
    final Optional<PatternMatch> match = matchImpl(dt, st, startToken, endToken);

    if (match.isPresent())
      return matchState.registerPatternMatch(this, m, match.get());
    else
      return matchState.registerUnmatched(this, m);
  }

  public PatternReturns match(DocTheory dt, SentenceTheory st, EventMention em,
                              PatternMatchState matchState, boolean fallThroughChildren) {

//    System.out.println("Matching regex pattern against event mention: " + em.tokenSpan().originalText().toString());

    final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, em);
    if (cachedMatch.isPresent()) {
      return cachedMatch.get();
    }

    TokenSequence.Span anchorSpan = em.span();
    TokenSequence.Span fullSpan = anchorSpan.extend(5);

    int startToken = fullSpan.startTokenIndexInclusive();
    int endToken = fullSpan.endTokenIndexInclusive();
    final Optional<PatternMatch> match = matchImpl(dt, st, startToken, endToken);

    if (match.isPresent())
      return matchState.registerPatternMatch(this, em, match.get());
    else
      return matchState.registerUnmatched(this, em);
  }

  // Match RegexPattern against span of sentence. Start and end token are inclusive.
  private Optional<PatternMatch> matchImpl(
      DocTheory dt, SentenceTheory st, int startToken, int endToken)
  {
    createRegex();

    TokenSequence ts = st.tokenSequence();
    StringBuilder stringToMatchAgainstBuilder = new StringBuilder();
    ImmutableList.Builder<Token> offsetToTokenMapBuilder = new ImmutableList.Builder();
    for (int i = startToken; i <= endToken; i++) {
      if (stringToMatchAgainstBuilder.length() != 0) {
        stringToMatchAgainstBuilder.append(" ");
        offsetToTokenMapBuilder.add(ts.token(i-1));
      }

      String txt = ts.token(i).tokenizedText().toString();
      stringToMatchAgainstBuilder.append(txt);
      for (int j = 0; j < txt.length(); j++) {
        offsetToTokenMapBuilder.add(ts.token(i));
      }
    }

    //System.out.println("Matching: "  + regexPattern.pattern() + " against: " + stringToMatchAgainstBuilder.toString());

    String stringToMatchAgainst = stringToMatchAgainstBuilder.toString();
    ImmutableList<Token> offsetToTokenMap = offsetToTokenMapBuilder.build();
    Matcher m = regexPattern.matcher(stringToMatchAgainst);
    if (!m.find()) {
      return Optional.absent();
    }

    Token matchStartToken = offsetToTokenMap.get(m.start());
    Token matchEndToken = offsetToTokenMap.get(m.end() - 1);

    final TokenSequence.Span tokenSeqSpan = ts.span(matchStartToken.index(), matchEndToken.index());
    final PatternMatch match = TokenSpanPatternMatch.of(this, dt, st, tokenSeqSpan);
    return Optional.of(match);
  }

  private void createRegex() {
    // Check if already initialized
    if (regexPattern != null)
      return;

    StringBuilder stringBuilder = new StringBuilder();

    // For figuring out which parenthetical inside of pattern corresponds to which match
    // for including mention patterns later (see C++ code)
    int submatchCount = 0;

    stringBuilder.append("\\b");
    for (Pattern p : subpatterns) {
      assert p instanceof TextPattern;
      TextPattern tp = (TextPattern) p;
      String text = tp.getText();
      submatchCount += StringUtils.countMatches(text, "(");
      submatchCount += StringUtils.countMatches(text, "\\(");
      stringBuilder.append("(");
      stringBuilder.append(text);
      stringBuilder.append(")");
      stringBuilder.append("\\b");
    }

    // TODO: Mention subpatterns (see C++ code)
    // TODO: filter regexes (see C++ code)

    regexPattern = java.util.regex.Pattern.compile(
        stringBuilder.toString(), java.util.regex.Pattern.CASE_INSENSITIVE);
  }

}
