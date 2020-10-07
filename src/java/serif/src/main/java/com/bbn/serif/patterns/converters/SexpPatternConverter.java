package com.bbn.serif.patterns.converters;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpAtom;
import com.bbn.bue.sexp.SexpList;
import com.bbn.serif.patterns.ArgumentPattern;
import com.bbn.serif.patterns.CombinationPattern;
import com.bbn.serif.patterns.ComparisonConstraint;
import com.bbn.serif.patterns.IntersectionPattern;
import com.bbn.serif.patterns.LabelPatternReturn;
import com.bbn.serif.patterns.LanguageVariantSwitchingPattern;
import com.bbn.serif.patterns.MapPatternReturn;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PatternFactory;
import com.bbn.serif.patterns.PatternReturn;
import com.bbn.serif.patterns.PatternSet;
import com.bbn.serif.patterns.PatternSetFactory;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.TextPattern;
import com.bbn.serif.patterns.UnionPattern;
import com.bbn.serif.patterns.ValueMentionPattern;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition.PredicateType;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;
import com.bbn.serif.types.ValueType;

import com.google.common.base.Function;

import java.util.Collection;

public class SexpPatternConverter extends PatternConverter<Sexp> {

  //Someday we may want to move these utility funcs to SexpUtils
  @SuppressWarnings("deprecation")
  private SexpList makePair(Symbol name, Sexp value) {
    SexpList result = new SexpList();
    result.add(new SexpAtom(name));
    result.add(value);
    return result;
  }

  private SexpList makePair(Symbol name, Symbol value) {
    return makePair(name, new SexpAtom(value));
  }

  private void addProperty(SexpList lst, Symbol name, Symbol value) {
    if (value != null) {
      lst.add(makePair(name, value));
    }
  }

  private void addProperty(SexpList lst, Symbol name, String value) {
    addProperty(lst, name, Symbol.from(value));
  }

  private void addNonZeroProperty(SexpList lst, Symbol name, Number value) {
    if (value.doubleValue() != 0.0) {
      addProperty(lst, name, value.toString());
    }
  }

  private void addBooleanProperty(SexpList lst, Symbol name, boolean value) {
    if (value) {
      lst.add(new SexpAtom(name));
    }
  }

  private final Function<Symbol, Sexp> convertSymbolToSexp =
      new Function<Symbol, Sexp>() {
        @Override
        public Sexp apply(Symbol symbol) {
          return new SexpAtom(Symbol.from(symbol.toString().replace("\"", "&quot;")));
        }
      };

  private final Function<EntityType, Sexp> convertEntityTypeToSexp =
      new Function<EntityType, Sexp>() {
        @Override
        public Sexp apply(EntityType entityType) {
          return new SexpAtom(entityType.name());
        }
      };

  private final Function<ValueType, Sexp> convertValueTypeToSexp =
      new Function<ValueType, Sexp>() {
        @Override
        public Sexp apply(ValueType valueType) {
          return new SexpAtom(valueType.name());
        }
      };

  private final Function<EntitySubtype, Sexp> convertEntitySubtypeToSexp =
      new Function<EntitySubtype, Sexp>() {
        @Override
        public Sexp apply(EntitySubtype subtype) {
          return new SexpAtom(subtype.name());
        }
      };

  private final Function<Mention.Type, Sexp> convertMentionTypeToSexp =
      new Function<Mention.Type, Sexp>() {
        @Override
        public Sexp apply(Mention.Type mentionType) {
          return new SexpAtom(mentionType.name());
        }
      };

  private final Function<Pattern, Sexp> convertPatternToSexp =
      new Function<Pattern, Sexp>() {
        @Override
        public Sexp apply(Pattern pattern) {
          return convert(pattern);
        }
      };

  private final Function<MentionPattern.ArgOfPropConstraint, Sexp> convertArgOfPropToSexp =
      new Function<MentionPattern.ArgOfPropConstraint, Sexp>() {
        @Override
        public Sexp apply(MentionPattern.ArgOfPropConstraint constraint) {
          SexpList result = initSexp(PatternFactory.ARG_OF_PROP_SYM);
          addCollectionProperty(result, PatternFactory.ROLE_SYM,
              constraint.getRoles(), convertSymbolToSexp);
          result.add(convert(constraint.getPropPattern()));
          return result;
        }
      };

  @SuppressWarnings("deprecation")
  private final Function<ComparisonConstraint, Sexp> convertComparisonToSexp =
      new Function<ComparisonConstraint, Sexp>() {
        @Override
        public Sexp apply(ComparisonConstraint constraint) {
          SexpList result = new SexpList();
          result.add(new SexpAtom(constraint.getConstraintType()));
          result.add(new SexpAtom(constraint.getComparisonOperator()));
          result.add(new SexpAtom(Symbol.from(constraint.getValue().toString())));
          return result;
        }
      };

  private <T> void addProperty(SexpList lst, Symbol name,
      T value, Function<T, Sexp> converter) {
    if (value != null) {
      lst.add(makePair(name, converter.apply(value)));
    }
  }

  private <T> void addCollectionProperty(SexpList lst, Symbol name,
      Collection<? extends T> value, Function<T, Sexp> converter) {
    if (!value.isEmpty()) {
      SexpList sublist = initSexp(name);
      for (T item : value) {
        sublist.add(converter.apply(item));
      }
      lst.add(sublist);
    }
  }

  private void addWildcardSymbolCollectionProperty(SexpList lst, Symbol name,
      Collection<Symbol> regular, Collection<Symbol> wildcard) {
    if (!regular.isEmpty() || !wildcard.isEmpty()) {
      SexpList sublist = initSexp(name);
      for (Symbol s : regular) {
        sublist.add(new SexpAtom(s));
      }
      for (Symbol s : wildcard) {
        Symbol newSym = Symbol.from(s.toString() + "*");
        sublist.add(new SexpAtom(newSym));
      }
      lst.add(sublist);
    }
  }

  @SuppressWarnings("deprecation")
  private final void addPatternReturnProperty(SexpList lst, PatternReturn patternReturn) {
    if (patternReturn instanceof LabelPatternReturn) {
      lst.add(makePair(PatternFactory.RETURN_SYM,
          ((LabelPatternReturn) patternReturn).getLabel()));
    } else if (patternReturn instanceof MapPatternReturn) {
      SexpList sublist = new SexpList();
      sublist.add(new SexpAtom(PatternFactory.RETURN_SYM));
      MapPatternReturn mapReturn = (MapPatternReturn) patternReturn;
      for (String key : mapReturn.keySet()) {
        sublist.add(makePair(Symbol.from(key),
            Symbol.from(mapReturn.get(key))));
      }
      lst.add(sublist);
    }
  }

  @SuppressWarnings("deprecation")
  private SexpList initSexp(Symbol type) {
    SexpList slist = new SexpList();
    slist.add(new SexpAtom(type));
    return slist;
  }

  private void handlePatternProperties(SexpList lst, Pattern p) {
    addProperty(lst, PatternFactory.ID_SYM, p.getId());
    addNonZeroProperty(lst, PatternFactory.SCORE_SYM, p.getScore());
    addPatternReturnProperty(lst, p.getPatternReturn());
    addProperty(lst, PatternFactory.SHORTCUT_SYM, p.getShortcut());
    addNonZeroProperty(lst, PatternFactory.SCORE_GROUP_SYM, p.getScoreGroup());
    addProperty(lst, PatternFactory.SCORE_FN_SYM, p.getScoreFunction());
    addBooleanProperty(lst, PatternFactory.TOP_LEVEL_RETURN_SYM, p.isTopLevelReturn());
  }

  private void handleLanguageVariantProperties(SexpList lst,
      LanguageVariantSwitchingPattern p) {
    addProperty(lst, PatternFactory.LANGUAGE_SYM, p.getLanguage());
    addProperty(lst, PatternFactory.VARIATION_SYM, p.getVariant());
  }

  public Sexp convert(PatternSet patternSet) {
    SexpList result = initSexp(patternSet.getPatternSetName());

    //handle wordsets
    SexpList wordsets = initSexp(PatternSetFactory.WORDSETS_SYM);
    for (Symbol wordset : patternSet.getWordSets().keySet()) {
      addCollectionProperty(wordsets, wordset,
          patternSet.getWordSets().get(wordset),
          convertSymbolToSexp);
    }
    result.add(wordsets);

    //handle entitylabels
    SexpList entitylabels = initSexp(PatternSetFactory.ENTITYLABELS_SYM);
    for (Symbol entitylabel : patternSet.getEntityLabels().keySet()) {
      addProperty(entitylabels, entitylabel,
          patternSet.getEntityLabels().get(entitylabel),
          convertPatternToSexp);
    }
    result.add(entitylabels);

    //handle reference patterns
    SexpList reference = initSexp(PatternSetFactory.REFERENCE_SYM);
    for (Symbol referencePattern : patternSet.getReferencePatterns().keySet()) {
      addProperty(reference, referencePattern,
          patternSet.getReferencePatterns().get(referencePattern),
          convertPatternToSexp);
    }
    result.add(reference);

    //handle doclevel patterns
    addCollectionProperty(result, PatternSetFactory.DOCLEVEL_SYM,
        patternSet.getDocPatterns(), convertPatternToSexp);

    //handle toplevel patterns
    addCollectionProperty(result, PatternSetFactory.TOPLEVEL_SYM,
        patternSet.getTopLevelPatterns(), convertPatternToSexp);

    return result;
  }

  @Override
  public Sexp convertArgumentPattern(ArgumentPattern pattern) {
    SexpList result = initSexp(PatternFactory.ARGUMENT_SYM);
    handlePatternProperties(result, pattern);
    addBooleanProperty(result, PatternFactory.OPTIONAL_SYM, pattern.isOptional());
    addCollectionProperty(result, PatternFactory.ROLE_SYM, pattern.getRoles(),
        convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.FALL_THROUGH_SYM,
        pattern.getFallThroughRoles(), convertSymbolToSexp);
    if (pattern.getPattern() != null) {
      result.add(convert(pattern.getPattern()));
    }
    return result;
  }

  @Override
  public Sexp convertCombinationPattern(CombinationPattern pattern) {
    SexpList result;
    if (pattern.getCombinationType().equals(CombinationPattern.CombinationType.ALL_OF)) {
      result = initSexp(PatternFactory.ALL_OF_SYM);
    } else if (pattern.getCombinationType().equals(CombinationPattern.CombinationType.ANY_OF)) {
      result = initSexp(PatternFactory.ANY_OF_SYM);
    } else {
      result = initSexp(PatternFactory.NONE_OF_SYM);
    }
    handlePatternProperties(result, pattern);
    addBooleanProperty(result, PatternFactory.GREEDY_SYM, pattern.isGreedy());
    addCollectionProperty(result, PatternFactory.MEMBERS_SYM,
        pattern.getPatternList(), convertPatternToSexp);
    return result;
  }

  @Override
  public Sexp convertIntersectionPattern(IntersectionPattern pattern) {
    SexpList result = initSexp(PatternFactory.INTERSECTION_SYM);
    handlePatternProperties(result, pattern);
    handleLanguageVariantProperties(result, pattern);
    addCollectionProperty(result, PatternFactory.MEMBERS_SYM,
        pattern.getPatternList(), convertPatternToSexp);
    return result;
  }

  @Override
  public Sexp convertMentionPattern(MentionPattern pattern) {
    SexpList result = initSexp(PatternFactory.MENTION_SYM);
    handlePatternProperties(result, pattern);
    handleLanguageVariantProperties(result, pattern);
    if (pattern.isRequiresName()) {
      addProperty(result, PatternFactory.MIN_ENTITYLEVEL_SYM, PatternFactory.NAME_SYM);
    } else if (pattern.isRequiresNameOrDesc()) {
      addProperty(result, PatternFactory.MIN_ENTITYLEVEL_SYM, PatternFactory.DESC_SYM);
    }

    addBooleanProperty(result, PatternFactory.BLOCK_FT_SYM, pattern.isBlockFallThrough());
    addBooleanProperty(result, PatternFactory.HEAD_REGEX_SYM, pattern.isHeadRegex());
    addBooleanProperty(result, PatternFactory.APPOSITIVE_SYM, pattern.isAppositive());
    addBooleanProperty(result, PatternFactory.APPOSITIVE_CHILD_SYM, pattern.isAppositiveChild());
    addBooleanProperty(result, PatternFactory.FOCUS_SYM, pattern.isFocus());
    addBooleanProperty(result, PatternFactory.GENERIC_SYM, pattern.isGeneric());
    addBooleanProperty(result, PatternFactory.NAMED_APPOSITIVE_CHILD_SYM,
        pattern.isNamedAppositive());
    addBooleanProperty(result, PatternFactory.SPECIFIC_SYM, pattern.isSpecific());

    addCollectionProperty(result, PatternFactory.ACESUBTYPE_SYM,
        pattern.getAceSubtypes(), convertEntitySubtypeToSexp);
    addCollectionProperty(result, PatternFactory.BLOCK_ACETYPE_SYM,
        pattern.getBlockedAceTypes(), convertEntityTypeToSexp);
    addWildcardSymbolCollectionProperty(result,
        PatternFactory.BLOCK_HEADWORD_SYM,
        pattern.getBlockedHeadwords(),
        pattern.getBlockedHeadwordPrefixes());
    addWildcardSymbolCollectionProperty(result,
        PatternFactory.HEADWORD_SYM,
        pattern.getHeadwords(),
        pattern.getHeadwordPrefixes());
    addCollectionProperty(result, PatternFactory.BLOCK_SYM,
        pattern.getBlockingEntityLabels(), convertSymbolToSexp);
    addProperty(result, PatternFactory.BROWN_CLUSTER_SYM, pattern.getBrownClusterConstraint());
    addCollectionProperty(result, PatternFactory.ENTITY_LABEL_SYM,
        pattern.getEntityLabels(), convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.ACETYPE_SYM,
        pattern.getEntityTypes(), convertEntityTypeToSexp);
    addCollectionProperty(result, PatternFactory.MENTIONTYPE_SYM,
        pattern.getMentionTypes(), convertMentionTypeToSexp);
    addProperty(result, PatternFactory.PROP_DEF_SYM,
        pattern.getPropDefPattern(), convertPatternToSexp);
    addProperty(result, PatternFactory.REGEX_SYM,
        pattern.getRegexPattern(), convertPatternToSexp);

    for (MentionPattern.ArgOfPropConstraint aop : pattern.getArgOfPropConstraints()) {
      result.add(convertArgOfPropToSexp.apply(aop));
    }
    for (ComparisonConstraint comp : pattern.getComparisonConstraints()) {
      result.add(convertComparisonToSexp.apply(comp));
    }
    return result;
  }

  @Override
  public Sexp convertPropPattern(PropPattern pattern) {
    SexpList result;
    if (pattern.getPredicateType().equals(PredicateType.NOUN)) {
      result = initSexp(PatternFactory.NPROP_SYM);
    } else if (pattern.getPredicateType().equals(PredicateType.VERB)) {
      result = initSexp(PatternFactory.VPROP_SYM);
    } else if (pattern.getPredicateType().equals(PredicateType.COMP)) {
      result = initSexp(PatternFactory.CPROP_SYM);
    } else if (pattern.getPredicateType().equals(PredicateType.SET)) {
      result = initSexp(PatternFactory.SPROP_SYM);
    } else if (pattern.getPredicateType().equals(PredicateType.MODIFIER)) {
      result = initSexp(PatternFactory.MPROP_SYM);
    } else {
      result = initSexp(PatternFactory.ANYPROP_SYM);
    }
    handlePatternProperties(result, pattern);
    handleLanguageVariantProperties(result, pattern);

    addWildcardSymbolCollectionProperty(result, PatternFactory.PREDICATE_SYM,
        pattern.getPredicates(), pattern.getPredicatePrefixes());
    addWildcardSymbolCollectionProperty(result, PatternFactory.MODAL_SYM,
        pattern.getModals(), pattern.getModalPrefixes());
    addWildcardSymbolCollectionProperty(result, PatternFactory.NEGATION_SYM,
        pattern.getNegations(), pattern.getNegationPrefixes());

    addCollectionProperty(result, PatternFactory.ALIGNED_PREDICATE_SYM,
        pattern.getAlignedPredicates(), convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.ADJECTIVE_SYM,
        pattern.getAdjectives(), convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.ADVERB_PARTICLE_SYM,
        pattern.getAdverbOrParticles(), convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.ARGS_SYM,
        pattern.getArgs(), convertPatternToSexp);
    addCollectionProperty(result, PatternFactory.BLOCK_ADJECTIVE_SYM,
        pattern.getBlockedAdjectives(), convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.BLOCK_ADVERB_PARTICLE_SYM,
        pattern.getBlockedAdverbsOrParticles(), convertSymbolToSexp);
    addCollectionProperty(result, PatternFactory.BLOCK_ARGS_SYM,
        pattern.getBlockedArgs(), convertPatternToSexp);
    addCollectionProperty(result, PatternFactory.OPT_ARGS_SYM,
        pattern.getOptArgs(), convertPatternToSexp);
    addCollectionProperty(result, PatternFactory.PARTICLE_SYM,
        pattern.getParticles(), convertSymbolToSexp);
    if (pattern.getPropModifierPattern() != null) {
      SexpList propMod = initSexp(PatternFactory.PROP_MODIFIER_SYM);
      addCollectionProperty(propMod, PatternFactory.ROLE_SYM,
          pattern.getPropModifierRoles(), convertSymbolToSexp);
      propMod.add(convert(pattern.getPropModifierPattern()));
    }
    addProperty(result, PatternFactory.REGEX_SYM,
        pattern.getRegexPattern(), convertPatternToSexp);

    addBooleanProperty(result, PatternFactory.MANY_TO_MANY_SYM,
        pattern.isAllowManyToManyMapping());
    addBooleanProperty(result, PatternFactory.NEGATIVE_SYM,
        pattern.isPsmManuallyInitialized());
    addBooleanProperty(result, PatternFactory.MATCH_ALL_ARGS_SYM,
        pattern.isRequireAllArgumentsToMatchSomePattern());
    addBooleanProperty(result, PatternFactory.ONE_TO_ONE_SYM,
        pattern.isRequireOneToOneArgumentMapping());
    addBooleanProperty(result, PatternFactory.STEM_PREDICATE_SYM,
        pattern.isStemPredicate());

    return result;
  }

  @Override
  public Sexp convertRegexPattern(RegexPattern pattern) {
    SexpList result = initSexp(PatternFactory.REGEX_SYM);
    handlePatternProperties(result, pattern);
    handleLanguageVariantProperties(result, pattern);
    addBooleanProperty(result, PatternFactory.DONT_ALLOW_HEADS_SYM, !pattern.isAllowHeads());
    addBooleanProperty(result, PatternFactory.DONT_ADD_SPACES_SYM, !pattern.isAddSpaces());
    addBooleanProperty(result, PatternFactory.MATCH_WHOLE_EXTENT_SYM, pattern.isMatchWholeExtent());
    addBooleanProperty(result, PatternFactory.TOP_MENTIONS_ONLY_SYM, pattern.isTopMentionsOnly());
    addCollectionProperty(result, PatternFactory.RE_SYM,
        pattern.getSubpatterns(), convertPatternToSexp);
    return result;
  }

  @Override
  public Sexp convertTextPattern(TextPattern pattern) {
    SexpList result = initSexp(PatternFactory.TEXT_SYM);
    handlePatternProperties(result, pattern);
    addBooleanProperty(result, PatternFactory.DONT_ADD_SPACES_SYM, !pattern.isAddSpaces());
    addBooleanProperty(result, PatternFactory.RAW_TEXT_SYM, pattern.isRawText());
    if (pattern.getText() != null) {
      addProperty(result, PatternFactory.STRING_SYM,
          "\"" + pattern.getText().replaceAll("\"", "&quot;") + "\"");
    }
    return result;
  }

  @Override
  public Sexp convertUnionPattern(UnionPattern pattern) {
    SexpList result = initSexp(PatternFactory.UNION_SYM);
    handlePatternProperties(result, pattern);
    handleLanguageVariantProperties(result, pattern);
    addBooleanProperty(result, PatternFactory.GREEDY_SYM, pattern.isGreedy());
    addCollectionProperty(result, PatternFactory.MEMBERS_SYM,
        pattern.getPatternList(), convertPatternToSexp);
    return result;
  }

  @Override
  public Sexp convertValuePattern(ValueMentionPattern pattern) {
    SexpList result = initSexp(PatternFactory.VALUE_SYM);
    handlePatternProperties(result, pattern);
    if (pattern.isMustBeSpecificDate()) {
      addBooleanProperty(result, PatternFactory.SPECIFIC_DATE_SYM, true);
    }
    if (pattern.isMustBeRecentDate()) {
      addBooleanProperty(result, PatternFactory.RECENT_DATE_SYM, true);
    }
    if (pattern.isMustBeFutureDate()) {
      addBooleanProperty(result, PatternFactory.FUTURE_DATE_SYM, true);
    }

    addCollectionProperty(result, PatternFactory.VALUE_TYPE_SYM,
        pattern.getValueTypes(), convertValueTypeToSexp);
    addProperty(result, PatternFactory.REGEX_SYM,
        pattern.getRegexPattern(), convertPatternToSexp);
    if (pattern.getActivityDateStatus() != null) {
      if (pattern.getActivityDateStatus().equals(ValueMentionPattern.DateStatus.IN_RANGE)) {
        addProperty(result, PatternFactory.ACTIVITY_DATE_SYM, PatternFactory.IN_RANGE_SYM);
      } else if (pattern.getActivityDateStatus()
          .equals(ValueMentionPattern.DateStatus.OUT_OF_RANGE)) {
        addProperty(result, PatternFactory.ACTIVITY_DATE_SYM, PatternFactory.OUT_OF_RANGE_SYM);
      } else if (pattern.getActivityDateStatus().equals(ValueMentionPattern.DateStatus.TOO_BROAD)) {
        addProperty(result, PatternFactory.ACTIVITY_DATE_SYM, PatternFactory.TOO_BROAD_SYM);
      } else {
        addProperty(result, PatternFactory.ACTIVITY_DATE_SYM, PatternFactory.NOT_SPECIFIC_SYM);
      }
    }
    for (ComparisonConstraint comp : pattern.getComparisonConstraints()) {
      result.add(convertComparisonToSexp.apply(comp));
    }
    return result;
  }

}
