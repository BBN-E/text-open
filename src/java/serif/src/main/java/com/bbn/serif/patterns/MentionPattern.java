package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MentionPattern extends LanguageVariantSwitchingPattern {

  private final List<ComparisonConstraint> comparisonConstraints;
  private final List<EntityType> aceTypes;
  private final List<EntitySubtype> aceSubtypes;
  private final List<EntityType> blockedAceTypes;
  private final Set<Mention.Type> mentionTypes;
  private final Set<Symbol> headwords;
  private final Set<Symbol> headwordPrefixes;
  private final Set<Symbol> blockedHeadwords;
  private final Set<Symbol> blockedHeadwordPrefixes;
  private final List<Symbol> entityLabels;
  private final List<Symbol> blockingEntityLabels;
  private final Pattern regexPattern;
  private final Pattern propDefPattern;
  private final boolean headRegex;
  private final List<ArgOfPropConstraint> argOfPropConstraints;
  private final Symbol brownClusterConstraint;
  private final long brownCluster;
  private final long brownClusterPrefix;
  private final boolean requiresName;
  private final boolean requiresNameOrDesc;
  private final boolean isSpecific;
  private final boolean isGeneric;
  private final boolean isAppositive;
  private final boolean isAppositiveChild;
  private final boolean isNamedAppositive;
  private final boolean blockFallThrough;
  private final boolean isFocus;

  public static final class ArgOfPropConstraint {

    private final Set<Symbol> roles;
    private final Pattern propPattern;

    public Set<Symbol> getRoles() {
      return roles;
    }

    public Pattern getPropPattern() {
      return propPattern;
    }

    public ArgOfPropConstraint(Set<Symbol> roles, Pattern propPattern) {
      this.roles = roles;
      this.propPattern = propPattern;
    }
  }

  /**
   * getter method for comparisonConstraint
   */
  public List<ComparisonConstraint> getComparisonConstraints() {
    return this.comparisonConstraints;
  }

  /**
   * getter method for entityTypes
   */
  public List<EntityType> getEntityTypes() {
    return this.aceTypes;
  }

  /**
   * getter method for aceSubtypes
   */
  public List<EntitySubtype> getAceSubtypes() {
    return this.aceSubtypes;
  }

  /**
   * getter method for blockedAceTypes
   */
  public List<EntityType> getBlockedAceTypes() {
    return this.blockedAceTypes;
  }

  /**
   * getter method for mentionTypes
   */
  public Set<Mention.Type> getMentionTypes() {
    return this.mentionTypes;
  }

  /**
   * getter method for headwords
   */
  public Set<Symbol> getHeadwords() {
    return this.headwords;
  }

  /**
   * getter method for headwordPrefixes
   */
  public Set<Symbol> getHeadwordPrefixes() {
    return this.headwordPrefixes;
  }

  /**
   * getter method for blockedHeadwords
   */
  public Set<Symbol> getBlockedHeadwords() {
    return this.blockedHeadwords;
  }

  /**
   * getter method for blockedHeadwordPrefixes
   */
  public Set<Symbol> getBlockedHeadwordPrefixes() {
    return this.blockedHeadwordPrefixes;
  }

  /**
   * getter method for entityLabels
   */
  public List<Symbol> getEntityLabels() {
    return this.entityLabels;
  }

  /**
   * getter method for blockingEntityLabels
   */
  public List<Symbol> getBlockingEntityLabels() {
    return this.blockingEntityLabels;
  }

  /**
   * getter method for regexPattern
   */
  public Pattern getRegexPattern() {
    return this.regexPattern;
  }

  /**
   * getter method for propDefPattern
   */
  public Pattern getPropDefPattern() {
    return this.propDefPattern;
  }

  /**
   * getter method for headRegex
   */
  public boolean isHeadRegex() {
    return this.headRegex;
  }

  /**
   * getter method for argOfPropConstraints
   */
  public List<ArgOfPropConstraint> getArgOfPropConstraints() {
    return this.argOfPropConstraints;
  }

  /**
   * getter method for brownClusterConstraint
   */
  public Symbol getBrownClusterConstraint() {
    return this.brownClusterConstraint;
  }

  /**
   * getter method for brownCluster
   */
  public long getBrownCluster() {
    return this.brownCluster;
  }

  /**
   * getter method for brownClusterPrefix
   */
  public long getBrownClusterPrefix() {
    return this.brownClusterPrefix;
  }

  /**
   * getter method for requiresName
   */
  public boolean isRequiresName() {
    return this.requiresName;
  }

  /**
   * getter method for requiresNameOrDesc
   */
  public boolean isRequiresNameOrDesc() {
    return this.requiresNameOrDesc;
  }

  /**
   * getter method for isSpecific
   */
  public boolean isSpecific() {
    return this.isSpecific;
  }

  /**
   * getter method for isGeneric
   */
  public boolean isGeneric() {
    return this.isGeneric;
  }

  /**
   * getter method for isAppositive
   */
  public boolean isAppositive() {
    return this.isAppositive;
  }

  /**
   * getter method for isAppositiveChild
   */
  public boolean isAppositiveChild() {
    return this.isAppositiveChild;
  }

  /**
   * getter method for isNamedAppositive
   */
  public boolean isNamedAppositive() {
    return this.isNamedAppositive;
  }

  /**
   * getter method for blockFallThrough
   */
  public boolean isBlockFallThrough() {
    return this.blockFallThrough;
  }

  /**
   * getter method for isFocus
   */
  public boolean isFocus() {
    return this.isFocus;
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder();
    super.modifiedCopyBuilder(b);
    b.withComparisonConstraints(comparisonConstraints);
    b.withAceTypes(aceTypes);
    b.withAceSubtypes(aceSubtypes);
    b.withBlockedAceTypes(blockedAceTypes);
    b.withMentionTypes(mentionTypes);
    b.withHeadwords(headwords);
    b.withBlockedHeadwords(blockedHeadwords);
    b.withHeadwordPrefixes(headwordPrefixes);
    b.withBlockedHeadwordPrefixes(blockedHeadwordPrefixes);
    b.withEntityLabels(entityLabels);
    b.withBlockingEntityLabels(blockingEntityLabels);
    b.withIsFocus(isFocus);
    b.withBlockFallThrough(blockFallThrough);
    b.withIsNamedAppositive(isNamedAppositive);
    b.withIsAppositiveChild(isAppositiveChild);
    b.withIsAppositive(isAppositive);
    b.withIsGeneric(isGeneric);
    b.withIsSpecific(isSpecific);
    b.withRequiresName(requiresName);
    b.withRequiresNameOrDesc(requiresNameOrDesc);
    b.withBrownClusterPrefix(brownClusterPrefix);
    b.withBrownCluster(brownCluster);
    b.withBrownClusterConstraint(brownClusterConstraint);
    b.withArgOfPropConstraints(argOfPropConstraints);
    b.withHeadRegex(headRegex);
    b.withPropDefPattern(propDefPattern);
    b.withRegexPattern(regexPattern);
    return b;
  }

  private MentionPattern(final Builder builder) {
    super(builder);
    this.comparisonConstraints = builder.comparisonConstraints;
    this.aceTypes = builder.aceTypes;
    this.aceSubtypes = builder.aceSubtypes;
    this.blockedAceTypes = builder.blockedAceTypes;
    this.mentionTypes = builder.mentionTypes;
    this.headwords = builder.headwords;
    this.headwordPrefixes = builder.headwordPrefixes;
    this.blockedHeadwords = builder.blockedHeadwords;
    this.blockedHeadwordPrefixes = builder.blockedHeadwordPrefixes;
    this.entityLabels = builder.entityLabels;
    this.blockingEntityLabels = builder.blockingEntityLabels;
    this.regexPattern = builder.regexPattern;
    this.propDefPattern = builder.propDefPattern;
    this.headRegex = builder.headRegex;
    this.argOfPropConstraints = builder.argOfPropConstraints;
    this.brownClusterConstraint = builder.brownClusterConstraint;
    this.brownCluster = builder.brownCluster;
    this.brownClusterPrefix = builder.brownClusterPrefix;
    this.requiresName = builder.requiresName;
    this.requiresNameOrDesc = builder.requiresNameOrDesc;
    this.isSpecific = builder.isSpecific;
    this.isGeneric = builder.isGeneric;
    this.isAppositive = builder.isAppositive;
    this.isAppositiveChild = builder.isAppositiveChild;
    this.isNamedAppositive = builder.isNamedAppositive;
    this.blockFallThrough = builder.blockFallThrough;
    this.isFocus = builder.isFocus;
  }

  public static final class Builder extends LanguageVariantSwitchingPattern.Builder {

    private List<ComparisonConstraint> comparisonConstraints;
    private List<EntityType> aceTypes;
    private List<EntitySubtype> aceSubtypes;
    private List<EntityType> blockedAceTypes;
    private Set<Mention.Type> mentionTypes;
    private Set<Symbol> headwords;
    private Set<Symbol> headwordPrefixes;
    private Set<Symbol> blockedHeadwords;
    private Set<Symbol> blockedHeadwordPrefixes;
    private List<Symbol> entityLabels;
    private List<Symbol> blockingEntityLabels;
    private Pattern regexPattern;
    private Pattern propDefPattern;
    private boolean headRegex;
    private List<ArgOfPropConstraint> argOfPropConstraints;
    private Symbol brownClusterConstraint;
    private long brownCluster;
    private long brownClusterPrefix;
    private boolean requiresName;
    private boolean requiresNameOrDesc;
    private boolean isSpecific;
    private boolean isGeneric;
    private boolean isAppositive;
    private boolean isAppositiveChild;
    private boolean isNamedAppositive;
    private boolean blockFallThrough;
    private boolean isFocus;

    public Builder() {
      this.comparisonConstraints = new ArrayList<ComparisonConstraint>();
      this.argOfPropConstraints = new ArrayList<ArgOfPropConstraint>();
      this.aceTypes = new ArrayList<EntityType>();
      this.blockedAceTypes = new ArrayList<EntityType>();
      this.aceSubtypes = new ArrayList<EntitySubtype>();
      this.mentionTypes = new HashSet<Mention.Type>();
      this.headwords = new HashSet<Symbol>();
      this.headwordPrefixes = new HashSet<Symbol>();
      this.blockedHeadwords = new HashSet<Symbol>();
      this.blockedHeadwordPrefixes = new HashSet<Symbol>();
      this.entityLabels = new ArrayList<Symbol>();
      this.blockingEntityLabels = new ArrayList<Symbol>();
    }

    @Override
    public MentionPattern build() {
      return new MentionPattern(this);
    }

    public Builder withComparisonConstraints(
        final List<ComparisonConstraint> comparisonConstraint) {
      this.comparisonConstraints = comparisonConstraint;
      return this;
    }

    public Builder withComparisonConstraintsAdd(final ComparisonConstraint comparisonConstraint) {
      this.comparisonConstraints.add(comparisonConstraint);
      return this;
    }

    public Builder withAceTypes(final List<EntityType> aceTypes) {
      this.aceTypes = aceTypes;
      return this;
    }

    public Builder withAceSubtypes(final List<EntitySubtype> aceSubtypes) {
      this.aceSubtypes = aceSubtypes;
      return this;
    }

    public Builder withBlockedAceTypes(final List<EntityType> blockedAceTypes) {
      this.blockedAceTypes = blockedAceTypes;
      return this;
    }

    public Builder withMentionTypes(final Set<Mention.Type> mentionTypes) {
      this.mentionTypes = mentionTypes;
      return this;
    }

    public Builder withHeadwords(final Set<Symbol> headwords) {
      this.headwords = headwords;
      return this;
    }

    public Builder withHeadwordPrefixes(final Set<Symbol> headwordPrefixes) {
      this.headwordPrefixes = headwordPrefixes;
      return this;
    }

    public Builder withBlockedHeadwords(final Set<Symbol> blockedHeadwords) {
      this.blockedHeadwords = blockedHeadwords;
      return this;
    }

    public Builder withBlockedHeadwordPrefixes(final Set<Symbol> blockedHeadwordPrefixes) {
      this.blockedHeadwordPrefixes = blockedHeadwordPrefixes;
      return this;
    }

    public Builder withEntityLabels(final List<Symbol> entityLabels) {
      this.entityLabels = entityLabels;
      return this;
    }

    public Builder withBlockingEntityLabels(final List<Symbol> blockingEntityLabels) {
      this.blockingEntityLabels = blockingEntityLabels;
      return this;
    }

    public Builder withRegexPattern(final Pattern regexPattern) {
      this.regexPattern = regexPattern;
      return this;
    }

    public Builder withPropDefPattern(final Pattern propDefPattern) {
      this.propDefPattern = propDefPattern;
      return this;
    }

    public Builder withHeadRegex(final boolean headRegex) {
      this.headRegex = headRegex;
      return this;
    }

    public Builder withArgOfPropConstraints(final List<ArgOfPropConstraint> argOfPropConstraints) {
      this.argOfPropConstraints = argOfPropConstraints;
      return this;
    }

    public Builder withArgOfPropConstraintsAdd(final ArgOfPropConstraint argOfPropConstraint) {
      this.argOfPropConstraints.add(argOfPropConstraint);
      return this;
    }

    public Builder withBrownClusterConstraint(final Symbol brownClusterConstraint) {
      this.brownClusterConstraint = brownClusterConstraint;
      return this;
    }

    public Builder withBrownCluster(final long brownCluster) {
      this.brownCluster = brownCluster;
      return this;
    }

    public Builder withBrownClusterPrefix(final long brownClusterPrefix) {
      this.brownClusterPrefix = brownClusterPrefix;
      return this;
    }

    public Builder withRequiresName(final boolean requiresName) {
      this.requiresName = requiresName;
      return this;
    }

    public Builder withRequiresNameOrDesc(final boolean requiresNameOrDesc) {
      this.requiresNameOrDesc = requiresNameOrDesc;
      return this;
    }

    public Builder withIsSpecific(final boolean isSpecific) {
      this.isSpecific = isSpecific;
      return this;
    }

    public Builder withIsGeneric(final boolean isGeneric) {
      this.isGeneric = isGeneric;
      return this;
    }

    public Builder withIsAppositive(final boolean isAppositive) {
      this.isAppositive = isAppositive;
      return this;
    }

    public Builder withIsAppositiveChild(final boolean isAppositiveChild) {
      this.isAppositiveChild = isAppositiveChild;
      return this;
    }

    public Builder withIsNamedAppositive(final boolean isNamedAppositive) {
      this.isNamedAppositive = isNamedAppositive;
      return this;
    }

    public Builder withBlockFallThrough(final boolean blockFallThrough) {
      this.blockFallThrough = blockFallThrough;
      return this;
    }

    public Builder withIsFocus(final boolean isFocus) {
      this.isFocus = isFocus;
      return this;
    }
  }

  public String toPrettyString() {
    StringBuilder sb = new StringBuilder();

    // Regex pattern (premods)
    StringBuilder regexSb = new StringBuilder();
    if (regexPattern != null) {
      regexSb.append(regexPattern.toPrettyString());
    }

    // Headwords
    StringBuilder headwordSb = new StringBuilder();
    if (headwords.size() > 1)
      headwordSb.append("[");
    boolean first = true;
    for (Symbol headword : headwords) {
      if (!first) headwordSb.append(" ");
      first = false;
      headwordSb.append(headword.asString());
    }
    if (headwords.size() > 1)
      headwordSb.append("]");

    // Acetypes
    StringBuilder aceTypeSb = new StringBuilder();
    if (aceTypes.size() > 0)
      aceTypeSb.append("[");
    first = true;
    for (EntityType aceType : aceTypes) {
      if (!first) aceTypeSb.append(" ");
      first = false;
      aceTypeSb.append(aceType.name());
    }
    if (aceTypes.size() > 0)
      aceTypeSb.append("]");

    sb.append(regexSb);
    sb.append(" ");
    sb.append(headwordSb.toString());
    sb.append(aceTypeSb.toString());
    sb.append(getPrettyReturnLabel());

    String prettyString = sb.toString();
    if (prettyString.trim().length() > 0)
      return prettyString;
    else
      return "[mention]";
  }

  @Override
  public int stableHashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result;
    result = prime * result
        + ((aceTypes == null) ? 0 : Pattern.stableHashCode(aceTypes));
    result = prime * result
        + ((headwords == null) ? 0 : Pattern.stableHashCode(headwords));
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((aceSubtypes == null) ? 0 : aceSubtypes.hashCode());
    result = prime * result
        + ((aceTypes == null) ? 0 : aceTypes.hashCode());
    result = prime
        * result
        + ((argOfPropConstraints == null) ? 0 : argOfPropConstraints
        .hashCode());
    result = prime * result + (blockFallThrough ? 1231 : 1237);
    result = prime * result
        + ((blockedAceTypes == null) ? 0 : blockedAceTypes.hashCode());
    result = prime
        * result
        + ((blockedHeadwordPrefixes == null) ? 0
                                             : blockedHeadwordPrefixes.hashCode());
    result = prime
        * result
        + ((blockedHeadwords == null) ? 0 : blockedHeadwords.hashCode());
    result = prime
        * result
        + ((blockingEntityLabels == null) ? 0 : blockingEntityLabels
        .hashCode());
    result = (int) (prime * result + brownCluster);
    result = prime
        * result
        + ((brownClusterConstraint == null) ? 0
                                            : brownClusterConstraint.hashCode());
    result = (int) (prime * result + brownClusterPrefix);
    result = prime
        * result
        + ((comparisonConstraints == null) ? 0 : comparisonConstraints
        .hashCode());
    result = prime * result
        + ((entityLabels == null) ? 0 : entityLabels.hashCode());
    result = prime * result + (headRegex ? 1231 : 1237);
    result = prime
        * result
        + ((headwordPrefixes == null) ? 0 : headwordPrefixes.hashCode());
    result = prime * result
        + ((headwords == null) ? 0 : headwords.hashCode());
    result = prime * result + (isAppositive ? 1231 : 1237);
    result = prime * result + (isAppositiveChild ? 1231 : 1237);
    result = prime * result + (isFocus ? 1231 : 1237);
    result = prime * result + (isGeneric ? 1231 : 1237);
    result = prime * result + (isNamedAppositive ? 1231 : 1237);
    result = prime * result + (isSpecific ? 1231 : 1237);
    result = prime * result
        + ((mentionTypes == null) ? 0 : mentionTypes.hashCode());
    result = prime * result
        + ((propDefPattern == null) ? 0 : propDefPattern.hashCode());
    result = prime * result
        + ((regexPattern == null) ? 0 : regexPattern.hashCode());
    result = prime * result + (requiresName ? 1231 : 1237);
    result = prime * result + (requiresNameOrDesc ? 1231 : 1237);
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
    MentionPattern other = (MentionPattern) obj;
    if (aceSubtypes == null) {
      if (other.aceSubtypes != null) {
        return false;
      }
    } else if (!aceSubtypes.equals(other.aceSubtypes)) {
      return false;
    }
    if (aceTypes == null) {
      if (other.aceTypes != null) {
        return false;
      }
    } else if (!aceTypes.equals(other.aceTypes)) {
      return false;
    }
    if (argOfPropConstraints == null) {
      if (other.argOfPropConstraints != null) {
        return false;
      }
    } else if (!argOfPropConstraints.equals(other.argOfPropConstraints)) {
      return false;
    }
    if (blockFallThrough != other.blockFallThrough) {
      return false;
    }
    if (blockedAceTypes == null) {
      if (other.blockedAceTypes != null) {
        return false;
      }
    } else if (!blockedAceTypes.equals(other.blockedAceTypes)) {
      return false;
    }
    if (blockedHeadwordPrefixes == null) {
      if (other.blockedHeadwordPrefixes != null) {
        return false;
      }
    } else if (!blockedHeadwordPrefixes
        .equals(other.blockedHeadwordPrefixes)) {
      return false;
    }
    if (blockedHeadwords == null) {
      if (other.blockedHeadwords != null) {
        return false;
      }
    } else if (!blockedHeadwords.equals(other.blockedHeadwords)) {
      return false;
    }
    if (blockingEntityLabels == null) {
      if (other.blockingEntityLabels != null) {
        return false;
      }
    } else if (!blockingEntityLabels.equals(other.blockingEntityLabels)) {
      return false;
    }
    if (brownCluster != other.brownCluster) {
      return false;
    }
    if (brownClusterConstraint == null) {
      if (other.brownClusterConstraint != null) {
        return false;
      }
    } else if (!brownClusterConstraint.equals(other.brownClusterConstraint)) {
      return false;
    }
    if (brownClusterPrefix != other.brownClusterPrefix) {
      return false;
    }
    if (comparisonConstraints == null) {
      if (other.comparisonConstraints != null) {
        return false;
      }
    } else if (!comparisonConstraints.equals(other.comparisonConstraints)) {
      return false;
    }
    if (entityLabels == null) {
      if (other.entityLabels != null) {
        return false;
      }
    } else if (!entityLabels.equals(other.entityLabels)) {
      return false;
    }
    if (headRegex != other.headRegex) {
      return false;
    }
    if (headwordPrefixes == null) {
      if (other.headwordPrefixes != null) {
        return false;
      }
    } else if (!headwordPrefixes.equals(other.headwordPrefixes)) {
      return false;
    }
    if (headwords == null) {
      if (other.headwords != null) {
        return false;
      }
    } else if (!headwords.equals(other.headwords)) {
      return false;
    }
    if (isAppositive != other.isAppositive) {
      return false;
    }
    if (isAppositiveChild != other.isAppositiveChild) {
      return false;
    }
    if (isFocus != other.isFocus) {
      return false;
    }
    if (isGeneric != other.isGeneric) {
      return false;
    }
    if (isNamedAppositive != other.isNamedAppositive) {
      return false;
    }
    if (isSpecific != other.isSpecific) {
      return false;
    }
    if (mentionTypes == null) {
      if (other.mentionTypes != null) {
        return false;
      }
    } else if (!mentionTypes.equals(other.mentionTypes)) {
      return false;
    }
    if (propDefPattern == null) {
      if (other.propDefPattern != null) {
        return false;
      }
    } else if (!propDefPattern.equals(other.propDefPattern)) {
      return false;
    }
    if (regexPattern == null) {
      if (other.regexPattern != null) {
        return false;
      }
    } else if (!regexPattern.equals(other.regexPattern)) {
      return false;
    }
    if (requiresName != other.requiresName) {
      return false;
    }
    if (requiresNameOrDesc != other.requiresNameOrDesc) {
      return false;
    }
    return true;
  }
}
