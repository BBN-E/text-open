package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.theories.Proposition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PropPattern extends LanguageVariantSwitchingPattern {

  private final Set<Symbol> predicates;
  private final Set<Symbol> predicatePrefixes;
  private final Set<Symbol> blockedPredicates;
  private final Set<Symbol> blockedPredicatePrefixes;
  private final Set<Symbol> particles;
  private final Set<Symbol> adjectives;
  private final Set<Symbol> blockedAdjectives;
  private final Set<Symbol> adverbOrParticles;
  private final Set<Symbol> blockedAdverbsOrParticles;
  private final Set<Symbol> propModifierRoles;
  private final Set<Symbol> negations;
  private final Set<Symbol> negationPrefixes;
  private final Set<Symbol> blockedNegations;
  private final Set<Symbol> blockedNegationPrefixes;
  private final Set<Symbol> modals;
  private final Set<Symbol> modalPrefixes;
  private final Set<Symbol> blockedModals;
  private final Set<Symbol> blockedModalPrefixes;
  private final boolean psmManuallyInitialized;
  private final Pattern propModifierPattern;
  private final Pattern regexPattern;
  private final Proposition.PredicateType predicateType;
  private final List<ArgumentPattern> args;
  private final List<ArgumentPattern> optArgs;
  private final List<ArgumentPattern> blockedArgs;
  private final boolean requireOneToOneArgumentMapping;
  private final boolean allowManyToManyMapping;
  private final boolean requireAllArgumentsToMatchSomePattern;
  private final boolean stemPredicate;
  private final Set<Symbol> alignedPredicates;

  public static final Set<Symbol> leftJustifiedRoles =
      new HashSet<>(
          Arrays.asList(Symbol.from("<unknown>"), Symbol.from("<sub>"), Symbol.from("<poss>")));


  /**
   * getter method for predicates
   */
  public Set<Symbol> getPredicates() {
    return this.predicates;
  }

  /**
   * getter method for predicatePrefixes
   */
  public Set<Symbol> getPredicatePrefixes() {
    return this.predicatePrefixes;
  }

  /**
   * getter method for blockedPredicates
   */
  public Set<Symbol> getBlockedPredicates() {
    return this.blockedPredicates;
  }

  /**
   * getter method for blockedPredicatePrefixes
   */
  public Set<Symbol> getBlockedPredicatePrefixes() {
    return this.blockedPredicatePrefixes;
  }

  /**
   * getter method for particles
   */
  public Set<Symbol> getParticles() {
    return this.particles;
  }

  /**
   * getter method for adjectives
   */
  public Set<Symbol> getAdjectives() {
    return this.adjectives;
  }

  /**
   * getter method for blockedAdjectives
   */
  public Set<Symbol> getBlockedAdjectives() {
    return this.blockedAdjectives;
  }

  /**
   * getter method for adverbOrParticles
   */
  public Set<Symbol> getAdverbOrParticles() {
    return this.adverbOrParticles;
  }

  /**
   * getter method for blockedAdverbsOrParticles
   */
  public Set<Symbol> getBlockedAdverbsOrParticles() {
    return this.blockedAdverbsOrParticles;
  }

  /**
   * getter method for propModifierRoles
   */
  public Set<Symbol> getPropModifierRoles() {
    return this.propModifierRoles;
  }

  /**
   * getter method for negations
   */
  public Set<Symbol> getNegations() {
    return this.negations;
  }

  /**
   * getter method for negationPrefixes
   */
  public Set<Symbol> getNegationPrefixes() {
    return this.negationPrefixes;
  }

  /**
   * getter method for blockedNegations
   */
  public Set<Symbol> getBlockedNegations() {
    return this.blockedNegations;
  }

  /**
   * getter method for blockedNegationPrefixes
   */
  public Set<Symbol> getBlockedNegationPrefixes() {
    return this.blockedNegationPrefixes;
  }

  /**
   * getter method for modals
   */
  public Set<Symbol> getModals() {
    return this.modals;
  }

  /**
   * getter method for modalPrefixes
   */
  public Set<Symbol> getModalPrefixes() {
    return this.modalPrefixes;
  }

  /**
   * getter method for blockedModals
   */
  public Set<Symbol> getBlockedModals() {
    return this.blockedModals;
  }

  /**
   * getter method for blockedModalPrefixes
   */
  public Set<Symbol> getBlockedModalPrefixes() {
    return this.blockedModalPrefixes;
  }

  /**
   * getter method for psmManuallyInitialized
   */
  public boolean isPsmManuallyInitialized() {
    return this.psmManuallyInitialized;
  }

  /**
   * getter method for propModifierPattern
   */
  public Pattern getPropModifierPattern() {
    return this.propModifierPattern;
  }

  /**
   * getter method for regexPattern
   */
  public Pattern getRegexPattern() {
    return this.regexPattern;
  }

  /**
   * getter method for predicateType
   */
  public Proposition.PredicateType getPredicateType() {
    return this.predicateType;
  }

  /**
   * getter method for args
   */
  public List<ArgumentPattern> getArgs() {
    return this.args;
  }

  /**
   * getter method for optArgs
   */
  public List<ArgumentPattern> getOptArgs() {
    return this.optArgs;
  }

  /**
   * getter method for blockedArgs
   */
  public List<ArgumentPattern> getBlockedArgs() {
    return this.blockedArgs;
  }

  /**
   * getter method for requireOneToOneArgumentMapping
   */
  public boolean isRequireOneToOneArgumentMapping() {
    return this.requireOneToOneArgumentMapping;
  }

  /**
   * getter method for allowManyToManyMapping
   */
  public boolean isAllowManyToManyMapping() {
    return this.allowManyToManyMapping;
  }

  /**
   * getter method for requireAllArgumentsToMatchSomePattern
   */
  public boolean isRequireAllArgumentsToMatchSomePattern() {
    return this.requireAllArgumentsToMatchSomePattern;
  }

  /**
   * getter method for stemPredicate
   */
  public boolean isStemPredicate() {
    return this.stemPredicate;
  }

  /**
   * getter method for alignedPredicates
   */
  public Set<Symbol> getAlignedPredicates() {
    return this.alignedPredicates;
  }

  private PropPattern(final Builder builder) {
    super(builder);
    this.predicates = builder.predicates;
    this.predicatePrefixes = builder.predicatePrefixes;
    this.blockedPredicates = builder.blockedPredicates;
    this.blockedPredicatePrefixes = builder.blockedPredicatePrefixes;
    this.particles = builder.particles;
    this.adjectives = builder.adjectives;
    this.blockedAdjectives = builder.blockedAdjectives;
    this.adverbOrParticles = builder.adverbOrParticles;
    this.blockedAdverbsOrParticles = builder.blockedAdverbsOrParticles;
    this.propModifierRoles = builder.propModifierRoles;
    this.negations = builder.negations;
    this.negationPrefixes = builder.negationPrefixes;
    this.blockedNegations = builder.blockedNegations;
    this.blockedNegationPrefixes = builder.blockedNegationPrefixes;
    this.modals = builder.modals;
    this.modalPrefixes = builder.modalPrefixes;
    this.blockedModals = builder.blockedModals;
    this.blockedModalPrefixes = builder.blockedModalPrefixes;
    this.psmManuallyInitialized = builder.psmManuallyInitialized;
    this.propModifierPattern = builder.propModifierPattern;
    this.regexPattern = builder.regexPattern;
    this.predicateType = builder.predicateType;
    this.args = builder.args;
    this.optArgs = builder.optArgs;
    this.blockedArgs = builder.blockedArgs;
    this.requireOneToOneArgumentMapping = builder.requireOneToOneArgumentMapping;
    this.allowManyToManyMapping = builder.allowManyToManyMapping;
    this.requireAllArgumentsToMatchSomePattern = builder.requireAllArgumentsToMatchSomePattern;
    this.stemPredicate = builder.stemPredicate;
    this.alignedPredicates = builder.alignedPredicates;
  }

  @Override
  public Builder modifiedCopyBuilder() {
    Builder b = new Builder(this.predicateType);
    super.modifiedCopyBuilder(b);
    b.withPredicates(predicates);
    b.withPredicatePrefixes(predicatePrefixes);
    b.withBlockedPredicates(blockedPredicates);
    b.withBlockedPredicatePrefixes(blockedPredicatePrefixes);
    b.withParticles(particles);
    b.withAdjectives(adjectives);
    b.withBlockedAdjectives(blockedAdjectives);
    b.withAdverbOrParticles(adverbOrParticles);
    b.withBlockedAdverbsOrParticles(blockedAdverbsOrParticles);
    b.withPropModifierRoles(propModifierRoles);
    b.withNegations(negations);
    b.withBlockedNegations(blockedNegations);
    b.withBlockedNegationPrefixes(blockedNegationPrefixes);
    b.withModals(modals);
    b.withModalPrefixes(modalPrefixes);
    b.withBlockedModals(blockedModals);
    b.withBlockedModalPrefixes(blockedModalPrefixes);
    b.withPsmManuallyInitialized(psmManuallyInitialized);
    b.withPropModifierPattern(propModifierPattern);
    b.withRegexPattern(regexPattern);
    b.withArgs(args);
    b.withOptArgs(optArgs);
    b.withBlockedArgs(blockedArgs);
    b.withRequireOneToOneArgumentMapping(requireOneToOneArgumentMapping);
    b.withAllowManyToManyMapping(allowManyToManyMapping);
    b.withRequireAllArgumentsToMatchSomePattern(requireAllArgumentsToMatchSomePattern);
    b.withStemPredicate(stemPredicate);
    b.withAlignedPredicates(alignedPredicates);
    return b;
  }

  public static class Builder extends LanguageVariantSwitchingPattern.Builder {

    private Set<Symbol> predicates;
    private Set<Symbol> predicatePrefixes;
    private Set<Symbol> blockedPredicates;
    private Set<Symbol> blockedPredicatePrefixes;
    private Set<Symbol> particles;
    private Set<Symbol> adjectives;
    private Set<Symbol> blockedAdjectives;
    private Set<Symbol> adverbOrParticles;
    private Set<Symbol> blockedAdverbsOrParticles;
    private Set<Symbol> propModifierRoles;
    private Set<Symbol> negations;
    private Set<Symbol> negationPrefixes;
    private Set<Symbol> blockedNegations;
    private Set<Symbol> blockedNegationPrefixes;
    private Set<Symbol> modals;
    private Set<Symbol> modalPrefixes;
    private Set<Symbol> blockedModals;
    private Set<Symbol> blockedModalPrefixes;
    private boolean psmManuallyInitialized;
    private Pattern propModifierPattern;
    private Pattern regexPattern;
    private final Proposition.PredicateType predicateType;
    private List<ArgumentPattern> args;
    private List<ArgumentPattern> optArgs;
    private List<ArgumentPattern> blockedArgs;
    private boolean requireOneToOneArgumentMapping;
    private boolean allowManyToManyMapping;
    private boolean requireAllArgumentsToMatchSomePattern;
    private boolean stemPredicate;
    private Set<Symbol> alignedPredicates;

    public Builder(Proposition.PredicateType type) {
      predicateType = type;
      this.predicates = new HashSet<Symbol>();
      this.alignedPredicates = new HashSet<Symbol>();
      this.predicatePrefixes = new HashSet<Symbol>();
      this.blockedPredicates = new HashSet<Symbol>();
      this.blockedPredicatePrefixes = new HashSet<Symbol>();
      this.particles = new HashSet<Symbol>();
      this.adjectives = new HashSet<Symbol>();
      this.blockedAdjectives = new HashSet<Symbol>();
      this.adverbOrParticles = new HashSet<Symbol>();
      this.blockedAdverbsOrParticles = new HashSet<Symbol>();
      this.propModifierRoles = new HashSet<Symbol>();
      this.negationPrefixes = new HashSet<Symbol>();
      this.negations = new HashSet<Symbol>();
      this.blockedNegations = new HashSet<Symbol>();
      this.blockedNegationPrefixes = new HashSet<Symbol>();
      this.modals = new HashSet<Symbol>();
      this.modalPrefixes = new HashSet<Symbol>();
      this.blockedModals = new HashSet<Symbol>();
      this.blockedModalPrefixes = new HashSet<Symbol>();
      this.args = new ArrayList<ArgumentPattern>();
      this.optArgs = new ArrayList<ArgumentPattern>();
      this.blockedArgs = new ArrayList<ArgumentPattern>();
    }

    @Override
    public PropPattern build() {
      return new PropPattern(this);
    }

    public Builder withPredicates(final Set<Symbol> predicates) {
      this.predicates = predicates;
      return this;
    }

    public Builder withPredicatePrefixes(final Set<Symbol> predicatePrefixes) {
      this.predicatePrefixes = predicatePrefixes;
      return this;
    }

    public Builder withBlockedPredicates(final Set<Symbol> blockedPredicates) {
      this.blockedPredicates = blockedPredicates;
      return this;
    }

    public Builder withBlockedPredicatePrefixes(final Set<Symbol> blockedPredicatePrefixes) {
      this.blockedPredicatePrefixes = blockedPredicatePrefixes;
      return this;
    }

    public Builder withParticles(final Set<Symbol> particles) {
      this.particles = particles;
      return this;
    }

    public Builder withAdjectives(final Set<Symbol> adjectives) {
      this.adjectives = adjectives;
      return this;
    }

    public Builder withBlockedAdjectives(final Set<Symbol> blockedAdjectives) {
      this.blockedAdjectives = blockedAdjectives;
      return this;
    }

    public Builder withAdverbOrParticles(final Set<Symbol> adverbOrParticles) {
      this.adverbOrParticles = adverbOrParticles;
      return this;
    }

    public Builder withBlockedAdverbsOrParticles(final Set<Symbol> blockedAdverbsOrParticles) {
      this.blockedAdverbsOrParticles = blockedAdverbsOrParticles;
      return this;
    }

    public Builder withPropModifierRoles(final Set<Symbol> propModifierRoles) {
      this.propModifierRoles = propModifierRoles;
      return this;
    }

    public Builder withNegations(final Set<Symbol> negations) {
      this.negations = negations;
      return this;
    }

    public Builder withNegationPrefixes(final Set<Symbol> negationPrefixes) {
      this.negationPrefixes = negationPrefixes;
      return this;
    }

    public Builder withBlockedNegations(final Set<Symbol> blockedNegations) {
      this.blockedNegations = blockedNegations;
      return this;
    }

    public Builder withBlockedNegationPrefixes(final Set<Symbol> blockedNegationPrefixes) {
      this.blockedNegationPrefixes = blockedNegationPrefixes;
      return this;
    }

    public Builder withModals(final Set<Symbol> modals) {
      this.modals = modals;
      return this;
    }

    public Builder withModalPrefixes(final Set<Symbol> modalPrefixes) {
      this.modalPrefixes = modalPrefixes;
      return this;
    }

    public Builder withBlockedModals(final Set<Symbol> blockedModals) {
      this.blockedModals = blockedModals;
      return this;
    }

    public Builder withBlockedModalPrefixes(final Set<Symbol> blockedModalPrefixes) {
      this.blockedModalPrefixes = blockedModalPrefixes;
      return this;
    }

    public Builder withPsmManuallyInitialized(final boolean psmManuallyInitialized) {
      this.psmManuallyInitialized = psmManuallyInitialized;
      return this;
    }

    public Builder withPropModifierPattern(final Pattern propModifierPattern) {
      this.propModifierPattern = propModifierPattern;
      return this;
    }

    public Builder withRegexPattern(final Pattern regexPattern) {
      this.regexPattern = regexPattern;
      return this;
    }

    public Builder withArgs(final List<ArgumentPattern> args) {
      List<ArgumentPattern> modifiableList = new ArrayList<>(args);
      Collections.sort(modifiableList);
      this.args = modifiableList;
      return this;
    }

    public Builder withOptArgs(final List<ArgumentPattern> optArgs) {
      this.optArgs = optArgs;
      return this;
    }

    public Builder withBlockedArgs(final List<ArgumentPattern> blockedArgs) {
      this.blockedArgs = blockedArgs;
      return this;
    }

    public Builder withRequireOneToOneArgumentMapping(
        final boolean requireOneToOneArgumentMapping) {
      this.requireOneToOneArgumentMapping = requireOneToOneArgumentMapping;
      return this;
    }

    public Builder withAllowManyToManyMapping(final boolean allowManyToManyMapping) {
      this.allowManyToManyMapping = allowManyToManyMapping;
      return this;
    }

    public Builder withRequireAllArgumentsToMatchSomePattern(
        final boolean requireAllArgumentsToMatchSomePattern) {
      this.requireAllArgumentsToMatchSomePattern = requireAllArgumentsToMatchSomePattern;
      return this;
    }

    public Builder withStemPredicate(final boolean stemPredicate) {
      this.stemPredicate = stemPredicate;
      return this;
    }

    public Builder withAlignedPredicates(final Set<Symbol> alignedPredicates) {
      this.alignedPredicates = alignedPredicates;
      return this;
    }
  }

  @Override
  public int stableHashCode() {
    final int prime = 31;
    int result = super.stableHashCode();
    result = prime * result + ((args == null) ? 0 : Pattern.stableHashCode(args));
    result = prime * result
        + ((predicateType == null) ? 0 : predicateType.stableHashCode());
    return result;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((adjectives == null) ? 0 : adjectives.hashCode());
    result = prime
        * result
        + ((adverbOrParticles == null) ? 0 : adverbOrParticles
        .hashCode());
    result = prime
        * result
        + ((alignedPredicates == null) ? 0 : alignedPredicates
        .hashCode());
    result = prime * result + (allowManyToManyMapping ? 1231 : 1237);
    result = prime * result + ((args == null) ? 0 : args.hashCode());
    result = prime
        * result
        + ((blockedAdjectives == null) ? 0 : blockedAdjectives
        .hashCode());
    result = prime
        * result
        + ((blockedAdverbsOrParticles == null) ? 0
                                               : blockedAdverbsOrParticles.hashCode());
    result = prime * result
        + ((blockedArgs == null) ? 0 : blockedArgs.hashCode());
    result = prime
        * result
        + ((blockedModalPrefixes == null) ? 0 : blockedModalPrefixes
        .hashCode());
    result = prime * result
        + ((blockedModals == null) ? 0 : blockedModals.hashCode());
    result = prime
        * result
        + ((blockedNegationPrefixes == null) ? 0
                                             : blockedNegationPrefixes.hashCode());
    result = prime
        * result
        + ((blockedNegations == null) ? 0 : blockedNegations.hashCode());
    result = prime
        * result
        + ((blockedPredicatePrefixes == null) ? 0
                                              : blockedPredicatePrefixes.hashCode());
    result = prime
        * result
        + ((blockedPredicates == null) ? 0 : blockedPredicates
        .hashCode());
    result = prime * result
        + ((modalPrefixes == null) ? 0 : modalPrefixes.hashCode());
    result = prime * result + ((modals == null) ? 0 : modals.hashCode());
    result = prime
        * result
        + ((negationPrefixes == null) ? 0 : negationPrefixes.hashCode());
    result = prime * result
        + ((negations == null) ? 0 : negations.hashCode());
    result = prime * result + ((optArgs == null) ? 0 : optArgs.hashCode());
    result = prime * result
        + ((particles == null) ? 0 : particles.hashCode());
    result = prime
        * result
        + ((predicatePrefixes == null) ? 0 : predicatePrefixes
        .hashCode());
    result = prime * result
        + ((predicateType == null) ? 0 : predicateType.hashCode());
    result = prime * result
        + ((predicates == null) ? 0 : predicates.hashCode());
    result = prime
        * result
        + ((propModifierPattern == null) ? 0 : propModifierPattern
        .hashCode());
    result = prime
        * result
        + ((propModifierRoles == null) ? 0 : propModifierRoles
        .hashCode());
    result = prime * result + (psmManuallyInitialized ? 1231 : 1237);
    result = prime * result
        + ((regexPattern == null) ? 0 : regexPattern.hashCode());
    result = prime * result
        + (requireAllArgumentsToMatchSomePattern ? 1231 : 1237);
    result = prime * result
        + (requireOneToOneArgumentMapping ? 1231 : 1237);
    result = prime * result + (stemPredicate ? 1231 : 1237);
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
    PropPattern other = (PropPattern) obj;
    if (adjectives == null) {
      if (other.adjectives != null) {
        return false;
      }
    } else if (!adjectives.equals(other.adjectives)) {
      return false;
    }
    if (adverbOrParticles == null) {
      if (other.adverbOrParticles != null) {
        return false;
      }
    } else if (!adverbOrParticles.equals(other.adverbOrParticles)) {
      return false;
    }
    if (alignedPredicates == null) {
      if (other.alignedPredicates != null) {
        return false;
      }
    } else if (!alignedPredicates.equals(other.alignedPredicates)) {
      return false;
    }
    if (allowManyToManyMapping != other.allowManyToManyMapping) {
      return false;
    }
    if (args == null) {
      if (other.args != null) {
        return false;
      }
    } else if (!args.equals(other.args)) {
      return false;
    }
    if (blockedAdjectives == null) {
      if (other.blockedAdjectives != null) {
        return false;
      }
    } else if (!blockedAdjectives.equals(other.blockedAdjectives)) {
      return false;
    }
    if (blockedAdverbsOrParticles == null) {
      if (other.blockedAdverbsOrParticles != null) {
        return false;
      }
    } else if (!blockedAdverbsOrParticles
        .equals(other.blockedAdverbsOrParticles)) {
      return false;
    }
    if (blockedArgs == null) {
      if (other.blockedArgs != null) {
        return false;
      }
    } else if (!blockedArgs.equals(other.blockedArgs)) {
      return false;
    }
    if (blockedModalPrefixes == null) {
      if (other.blockedModalPrefixes != null) {
        return false;
      }
    } else if (!blockedModalPrefixes.equals(other.blockedModalPrefixes)) {
      return false;
    }
    if (blockedModals == null) {
      if (other.blockedModals != null) {
        return false;
      }
    } else if (!blockedModals.equals(other.blockedModals)) {
      return false;
    }
    if (blockedNegationPrefixes == null) {
      if (other.blockedNegationPrefixes != null) {
        return false;
      }
    } else if (!blockedNegationPrefixes
        .equals(other.blockedNegationPrefixes)) {
      return false;
    }
    if (blockedNegations == null) {
      if (other.blockedNegations != null) {
        return false;
      }
    } else if (!blockedNegations.equals(other.blockedNegations)) {
      return false;
    }
    if (blockedPredicatePrefixes == null) {
      if (other.blockedPredicatePrefixes != null) {
        return false;
      }
    } else if (!blockedPredicatePrefixes
        .equals(other.blockedPredicatePrefixes)) {
      return false;
    }
    if (blockedPredicates == null) {
      if (other.blockedPredicates != null) {
        return false;
      }
    } else if (!blockedPredicates.equals(other.blockedPredicates)) {
      return false;
    }
    if (modalPrefixes == null) {
      if (other.modalPrefixes != null) {
        return false;
      }
    } else if (!modalPrefixes.equals(other.modalPrefixes)) {
      return false;
    }
    if (modals == null) {
      if (other.modals != null) {
        return false;
      }
    } else if (!modals.equals(other.modals)) {
      return false;
    }
    if (negationPrefixes == null) {
      if (other.negationPrefixes != null) {
        return false;
      }
    } else if (!negationPrefixes.equals(other.negationPrefixes)) {
      return false;
    }
    if (negations == null) {
      if (other.negations != null) {
        return false;
      }
    } else if (!negations.equals(other.negations)) {
      return false;
    }
    if (optArgs == null) {
      if (other.optArgs != null) {
        return false;
      }
    } else if (!optArgs.equals(other.optArgs)) {
      return false;
    }
    if (particles == null) {
      if (other.particles != null) {
        return false;
      }
    } else if (!particles.equals(other.particles)) {
      return false;
    }
    if (predicatePrefixes == null) {
      if (other.predicatePrefixes != null) {
        return false;
      }
    } else if (!predicatePrefixes.equals(other.predicatePrefixes)) {
      return false;
    }
    if (predicateType == null) {
      if (other.predicateType != null) {
        return false;
      }
    } else if (!predicateType.equals(other.predicateType)) {
      return false;
    }
    if (predicates == null) {
      if (other.predicates != null) {
        return false;
      }
    } else if (!predicates.equals(other.predicates)) {
      return false;
    }
    if (propModifierPattern == null) {
      if (other.propModifierPattern != null) {
        return false;
      }
    } else if (!propModifierPattern.equals(other.propModifierPattern)) {
      return false;
    }
    if (propModifierRoles == null) {
      if (other.propModifierRoles != null) {
        return false;
      }
    } else if (!propModifierRoles.equals(other.propModifierRoles)) {
      return false;
    }
    if (psmManuallyInitialized != other.psmManuallyInitialized) {
      return false;
    }
    if (regexPattern == null) {
      if (other.regexPattern != null) {
        return false;
      }
    } else if (!regexPattern.equals(other.regexPattern)) {
      return false;
    }
    if (requireAllArgumentsToMatchSomePattern != other.requireAllArgumentsToMatchSomePattern) {
      return false;
    }
    if (requireOneToOneArgumentMapping != other.requireOneToOneArgumentMapping) {
      return false;
    }
    if (stemPredicate != other.stemPredicate) {
      return false;
    }
    return true;
  }

  private boolean hasRefArg() {
    for (ArgumentPattern ap : args) {
      if (ap.getRoles().size() == 1 && ap.getRoles().get(0) == Symbol.from("<ref>"))
        return true;
    }
    return false;
  }

  private String getMPropPrettyString() {
    StringBuilder sb = new StringBuilder();

    String predHead = getPrettyPredicateHead();
    String predicateType = getPrettyPredicateType();

    StringBuilder headSb = new StringBuilder();
    headSb.append(" ");
    headSb.append(predicateType);
    headSb.append(predHead);
    headSb.append(getPrettyReturnLabel());
    headSb.append(" ");

    boolean replaced = false;
    for (ArgumentPattern ap : args) {
      String argString = " " + ap.toPrettyString();

      if (!replaced && predHead.trim().length() > 0 && argString.contains(" " + predHead)) {
        replaced = true;
        argString = argString.replace(" " + predHead, " " + headSb.toString());
      }

      if (ap.getRoles().size() == 1 && ap.getRoles().get(0) == Symbol.from("<ref>")) {
        argString = argString.replace("<ref>", " ");
      }

      sb.append(argString);
    }

    // Must add in predHead
    String finalString;
    if (!replaced)
      finalString = " [ " + headSb.toString() + sb.toString() + " ]";
    else
      finalString = " [ " + sb.toString() + " ]";

    return finalString.replaceAll("\\s+", " ");
  }

  public String toPrettyString() {

    if (predicateType == Proposition.PredicateType.MODIFIER) {
      return getMPropPrettyString();
    }

    StringBuilder sb = new StringBuilder();
    sb.append(" [ ");
    // Left justified arguments
    for (ArgumentPattern ap : args) {
      if (ap.getRoles().size() > 0 && leftJustifiedRoles.contains(ap.getRoles().get(0))) {
        sb.append(" ");
        sb.append(ap.toPrettyString());
        sb.append(" ");
      }
    }

    String premodsFromReferent = getPremodsFromReferent();
    if (premodsFromReferent != null) {
      sb.append(" ");
      sb.append(premodsFromReferent);
      sb.append(" ");
    }

    sb.append(" ");
    sb.append(getPrettyPredicateType());
    sb.append(getPrettyPredicateHead());
    sb.append(getPrettyReturnLabel());

    // Right justified arguments
    for (ArgumentPattern ap : args) {
      if (ap.getRoles().size() == 1 &&
          ap.getRoles().get(0) == Symbol.from("<ref>") &&
          premodsFromReferent != null)
      {
        // We already printed out the relevant information for this
        continue;
      }

      if (ap.getRoles().size() == 0 ||
          !leftJustifiedRoles.contains(ap.getRoles().get(0)))
      {
        sb.append(" ");
        sb.append(ap.toPrettyString());
      }
    }

    sb.append(" ]");
    return sb.toString().replaceAll("\\s+", " ");
  }

  private String getPremodsFromReferent() {
    for (ArgumentPattern ap : args) {
      if (ap.getRoles().size() == 1 &&
          ap.getRoles().get(0) == Symbol.from("<ref>") &&
          predicateType == Proposition.PredicateType.NOUN &&
          ap.getPattern() != null &&
          ap.getPattern() instanceof MentionPattern &&
          ap.getPattern().getPatternReturn() == null)
      {
        // We have a <ref> that we don't want to print out normally
        MentionPattern mp = (MentionPattern) ap.getPattern();
        RegexPattern rp = (RegexPattern) mp.getRegexPattern();
        if (rp != null)
          return rp.toPrettyString();
        return "";
      }
    }
    return null;
  }

  private String getPrettyPredicateHead() {
    StringBuilder sb = new StringBuilder();
    if (predicates.size() == 0)
      return "";
    else {
      boolean first = true;
      for (Symbol s : predicates) {
        if (!first)
          sb.append(" ");
        first = false;
        sb.append(s.asString());
      }
      return sb.toString();
    }
  }

  private String getPrettyPredicateType() {
    if (predicateType == Proposition.PredicateType.VERB)
      return "[v]";
    else if (predicateType == Proposition.PredicateType.MODIFIER)
      return "[m]";
    else if (predicateType == Proposition.PredicateType.NOUN)
      return "[n]";
    else if (predicateType == Proposition.PredicateType.SET)
      return "[s]";
    else if (predicateType == Proposition.PredicateType.COMP)
      return "[c]";
    else
      return "[" + predicateType.toString() + "]";
  }
}
