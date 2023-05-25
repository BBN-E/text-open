package com.bbn.serif.patterns;

import com.bbn.bue.common.HasStableHashCode;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.matching.ArgumentMatchingPattern;
import com.bbn.serif.patterns.matching.ArgumentPatternMatch;
import com.bbn.serif.patterns.matching.ArgumentValueMatchingPattern;
import com.bbn.serif.patterns.matching.EventArgumentMatchingPattern;
import com.bbn.serif.patterns.matching.EventMatchingPattern;
import com.bbn.serif.patterns.matching.EventPatternMatch;
import com.bbn.serif.patterns.matching.PatternMatch;
import com.bbn.serif.patterns.matching.PatternMatchState;
import com.bbn.serif.patterns.matching.PatternReturns;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class ArgumentPattern extends Pattern
    implements Comparable<ArgumentPattern>, HasStableHashCode, ArgumentValueMatchingPattern, EventArgumentMatchingPattern
{

  private final List<Symbol> fallThroughRoles;
  private final List<Symbol> roles;
  private final Pattern pattern;
  private final boolean optional;

  /**
   * getter method for roles
   */
  public List<Symbol> getRoles() {
    return this.roles;
  }

  /**
   * getter method for pattern
   */
  public Pattern getPattern() {
    return this.pattern;
  }

  /**
   * getter method for optional
   */
  public boolean isOptional() {
    return this.optional;
  }

  /**
   * getter method for fallThroughRoles
   */
  public List<Symbol> getFallThroughRoles() {
    return this.fallThroughRoles;
  }

  private ArgumentPattern(final Builder builder) {
    super(builder);
    this.roles = ImmutableList.copyOf(builder.roles);
    this.pattern = builder.pattern;
    this.optional = builder.optional;
    this.fallThroughRoles = ImmutableList.copyOf(builder.fallThroughRoles);
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withRoles(roles);
    b.withPattern(pattern);
    b.withOptional(optional);
    b.withFallThroughRoles(fallThroughRoles);
    return b;
  }

  public static class Builder extends Pattern.Builder {

    private List<Symbol> roles;
    private Pattern pattern;
    private boolean optional;
    private List<Symbol> fallThroughRoles;

    public Builder() {
      fallThroughRoles = new ArrayList<Symbol>();
      roles = new ArrayList<Symbol>();
    }

    @Override
    public ArgumentPattern build() {
      return new ArgumentPattern(this);
    }

    public Builder withRoles(final List<Symbol> roles) {
      this.roles = roles;
      return this;
    }

    public Builder withPattern(final Pattern pattern) {
      this.pattern = pattern;
      return this;
    }

    public Builder withOptional(final boolean optional) {
      this.optional = optional;
      return this;
    }

    public Builder withFallThroughRoles(final List<Symbol> fallThroughRoles) {
      this.fallThroughRoles = fallThroughRoles;
      return this;
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime
        * result
        + ((fallThroughRoles == null) ? 0 : fallThroughRoles.hashCode());
    result = prime * result + (optional ? 1231 : 1237);
    result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
    result = prime * result + ((roles == null) ? 0 : roles.hashCode());
    return result;
  }

  @Override
  public int stableHashCode() {
    final int prime = 31;
    int result = super.stableHashCode();
    result = prime
        * result
        + ((fallThroughRoles == null) ? 0 : Pattern.stableHashCode(fallThroughRoles));
    result = prime * result + (optional ? 1231 : 1237);
    result = prime * result + ((pattern == null) ? 0 : pattern.stableHashCode());
    result = prime * result + ((roles == null) ? 0 : Pattern.stableHashCode(roles));
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
    ArgumentPattern other = (ArgumentPattern) obj;
    if (fallThroughRoles == null) {
      if (other.fallThroughRoles != null) {
        return false;
      }
    } else if (!fallThroughRoles.equals(other.fallThroughRoles)) {
      return false;
    }
    if (optional != other.optional) {
      return false;
    }
    if (pattern == null) {
      if (other.pattern != null) {
        return false;
      }
    } else if (!pattern.equals(other.pattern)) {
      return false;
    }
    if (roles == null) {
      if (other.roles != null) {
        return false;
      }
    } else if (!roles.equals(other.roles)) {
      return false;
    }
    return true;
  }

  public int compareTo(ArgumentPattern other) {
    if (stableHashCode() < other.stableHashCode())
      return -1;
    if (stableHashCode() > other.stableHashCode())
      return 1;
    return 0;
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();

    StringBuilder rolesSb = new StringBuilder();
    for (Symbol role : roles) {
      if (role != Symbol.from("<unknown>")) {
        rolesSb.append(" ");
        rolesSb.append(role.asString());
      }
    }
    String role = rolesSb.toString();
    if (roles.size() == 0)
      role = " <any>";

    if (role.trim().equals("<sub>") || role.trim().equals("<poss>")) {
      if (pattern != null) {
        sb.append(" ");
        sb.append(pattern.toPrettyString());
      }
      sb.append(role);
    } else {
      sb.append(role);
      sb.append(" ");
      if (pattern != null) {
        sb.append(pattern.toPrettyString());
      }
    }

    String prettyString = sb.toString();
    return prettyString.replaceAll("\\s+", " ");
  }

  public String toDPropPrettyString() {
    StringBuilder sb = new StringBuilder();

    StringBuilder rolesSb = new StringBuilder();
    for (Symbol role : roles) {
      if (role != Symbol.from("<unknown>")) {
        rolesSb.append(" ");
        rolesSb.append(role.asString());
      }
    }
    String role = rolesSb.toString();
    if (roles.size() == 0)
      role = " <any>";

    if (role.trim().equals("<ref>")) {
      sb.append(" ");
      if (pattern != null) {
        sb.append(pattern.toPrettyString());
      }
    } else if (role.trim().equals("nmod") || role.trim().equals("nmod:tmod")) {
      sb.append(" [of/to/by/per] ");
      if (pattern != null) {
        sb.append(pattern.toPrettyString());
      }
    } else if (role.trim().equals("xcomp")) {
      sb.append(" [to] ");
      if (pattern != null) {
        sb.append(pattern.toPrettyString());
      }
    } else if (role.trim().equals("nsubj:pass")) {
      if (pattern != null) {
        sb.append(" ");
        sb.append(pattern.toPrettyString());
      }
      sb.append(" [is/are] ");
    } else if (role.trim().equals("nmod:poss")) {
      if (pattern != null) {
        sb.append(" ");
        sb.append(pattern.toPrettyString());
      }
      sb.append(" ['s] ");
    } else if (role.trim().equals("obl") || role.trim().equals("obl:tmod")) {
      sb.append(" [to/in/on/by/from] ");
      if (pattern != null) {
        sb.append(" ");
        sb.append(pattern.toPrettyString());
      }
    } else if (role.trim().equals("conj")) {
      sb.append(" [and/or] ");
      if (pattern != null) {
        sb.append(" ");
        sb.append(pattern.toPrettyString());
      }
    } else {
      //role.trim().equals("<ref>") || role.trim().equals("amod") || role.trim().equals("nsubj") ||
      //role.trim().equals("obj") ||  role.trim().equals("acl") || role.trim().equals("compound") ||
      //role.trim().equals("acl:relcl") || role.trim().equals("nummod") || role.trim().equals("flat") ||
      //role.trim().equals("compound:prt") || role.trim().equals("fixed"))
      //sb.append(role);
      sb.append(" ");
      if (pattern != null) {
        sb.append(pattern.toPrettyString());
      }
    }

    String prettyString = sb.toString();
    return prettyString.replaceAll("\\s+", " ");
  }

  @Override
  public PatternReturns match(DocTheory dt, SentenceTheory st, Proposition.Argument a,
      PatternMatchState matchState, boolean fallThroughChildren) {
    final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, a);
    if (cachedMatch.isPresent()) {
      return cachedMatch.get();
    }

    final boolean matches = passesRoleConstraint(a)
        && passesArgumentValueConstraint(a, dt, st, matchState, fallThroughChildren);

    if (matches) {
      final PatternMatch match = ArgumentPatternMatch.of(this, dt, st, a);
      return matchState.registerPatternMatch(this, a, match);
    } else {
      return matchState.registerUnmatched(this, a);
    }
  }


  @Override
  public PatternReturns match(DocTheory dt, SentenceTheory st, EventEventRelationMention.Argument a,
                              PatternMatchState matchState, boolean fallThroughChildren) {
    final Optional<PatternReturns> cachedMatch = matchState.cachedMatches(this, a);
    if (cachedMatch.isPresent()) {
      return cachedMatch.get();
    }

    if (a instanceof EventEventRelationMention.EventMentionArgument) {
      EventEventRelationMention.EventMentionArgument ema = (EventEventRelationMention.EventMentionArgument) a;

      final boolean matches = passesRoleConstraint(a)
              && passesEventArgumentConstraint(ema, dt, st, matchState, fallThroughChildren);
      if (matches) {
        final PatternMatch match = EventPatternMatch.of(this, dt, st, ema.eventMention());
        return matchState.registerPatternMatch(this, a, match);
      }
    }
    return matchState.registerUnmatched(this, a);
  }

  private boolean passesArgumentValueConstraint(Proposition.Argument a, DocTheory dt, SentenceTheory st,
      PatternMatchState matchState, boolean fallThroughChildren)
  {
    if (pattern == null)
      return true;

    if (!(pattern instanceof ArgumentValueMatchingPattern))
      return false;

    ArgumentValueMatchingPattern avmp = (ArgumentValueMatchingPattern) pattern;
    PatternReturns avmpPatternReturns = avmp.match(dt, st, a, matchState, fallThroughChildren);
    return avmpPatternReturns.matched();

  }

  private boolean passesEventArgumentConstraint(EventEventRelationMention.EventMentionArgument a,
                                                DocTheory dt, SentenceTheory st,
                                                PatternMatchState matchState, boolean fallThroughChildren)
  {
    if (pattern == null)
      return true;

    if (!(pattern instanceof EventMatchingPattern))
      return false;

    EventMatchingPattern emp = (EventMatchingPattern) pattern;
    PatternReturns empPatternReturns = emp.match(dt, st, a.eventMention(), matchState, fallThroughChildren);
    return empPatternReturns.matched();

  }

  private boolean passesRoleConstraint(Proposition.Argument a) {
    return roles.size() == 0 || (a.role().isPresent() && roles.contains(a.role().get()));
  }

  private boolean passesRoleConstraint(EventEventRelationMention.Argument a) {
    return roles.size() == 0 || roles.contains(a.role());
  }

}
