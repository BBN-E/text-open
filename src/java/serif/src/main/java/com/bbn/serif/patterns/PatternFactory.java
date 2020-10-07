package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpAtom;
import com.bbn.bue.sexp.SexpList;
import com.bbn.bue.sexp.SexpUtils;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Proposition.PredicateType;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;
import com.bbn.serif.types.ValueType;

import com.google.common.base.Function;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PatternFactory {

  //Abstract Pattern
  public final static Symbol SCORE_SYM = Symbol.from("score");
  public final static Symbol SHORTCUT_SYM = Symbol.from("shortcut");
  public final static Symbol ID_SYM = Symbol.from("id");
  public final static Symbol RETURN_SYM = Symbol.from("return");
  public final static Symbol SCORE_GROUP_SYM = Symbol.from("score_group");
  public final static Symbol SCORE_FN_SYM = Symbol.from("score-fn");
  public final static Symbol TOP_LEVEL_RETURN_SYM = Symbol.from("toplevel-return");

  //Pattern Types
  public final static Symbol ALL_OF_SYM = Symbol.from("all-of");
  public final static Symbol ANY_OF_SYM = Symbol.from("any-of");
  public final static Symbol NONE_OF_SYM = Symbol.from("none-of");
  public final static Symbol ARGUMENT_SYM = Symbol.from("argument");
  public final static Symbol DOC_SYM = Symbol.from("doc");
  public final static Symbol EVENT_SYM = Symbol.from("event");
  public final static Symbol INTERSECTION_SYM = Symbol.from("intersection");
  public final static Symbol MENTION_SYM = Symbol.from("mention");
  public final static Symbol NEGATION_SYM = Symbol.from("negation");
  public final static Symbol PARSE_NODE_SYM = Symbol.from("parse-node");
  public final static Symbol VPROP_SYM = Symbol.from("vprop");
  public final static Symbol NPROP_SYM = Symbol.from("nprop");
  public final static Symbol MPROP_SYM = Symbol.from("mprop");
  public final static Symbol CPROP_SYM = Symbol.from("cprop");
  public final static Symbol SPROP_SYM = Symbol.from("sprop");
  public final static Symbol ANYPROP_SYM = Symbol.from("anyprop");
  public final static Symbol QUOTATION_SYM = Symbol.from("quotation");
  public final static Symbol REGEX_SYM = Symbol.from("regex");
  public final static Symbol RELATION_SYM = Symbol.from("relation");
  public final static Symbol TEXT_SYM = Symbol.from("text");
  public final static Symbol TOPIC_SYM = Symbol.from("topic");
  public final static Symbol UNION_SYM = Symbol.from("union");
  public final static Symbol VALUE_SYM = Symbol.from("value");
  public final static Symbol LANGUAGE_SYM = Symbol.from("language");
  public final static Symbol VARIATION_SYM = Symbol.from("variation");

  //MentionPattern
  public final static Symbol NAME_SYM = Symbol.from("NAME");
  public final static Symbol DESC_SYM = Symbol.from("DESC");
  public final static Symbol BLOCK_SYM = Symbol.from("block");
  public final static Symbol FOCUS_SYM = Symbol.from("FOCUS");
  public final static Symbol ACETYPE_SYM = Symbol.from("acetype");
  public final static Symbol ACESUBTYPE_SYM = Symbol.from("acesubtype");
  public final static Symbol MENTIONTYPE_SYM = Symbol.from("mentiontype");
  public final static Symbol MIN_ENTITYLEVEL_SYM = Symbol.from("min-entitylevel");
  public final static Symbol ENTITY_LABEL_SYM = Symbol.from("entitylabel");
  public final static Symbol MENTION_LABEL_SYM = Symbol.from("mentionlabel");
  public final static Symbol HEADWORD_SYM = Symbol.from("headword");
  public final static Symbol BLOCK_ACETYPE_SYM = Symbol.from("block_acetype");
  public final static Symbol BLOCK_HEADWORD_SYM = Symbol.from("block_headword");
  public final static Symbol HEAD_REGEX_SYM = Symbol.from("head-regex");
  public final static Symbol SPECIFIC_SYM = Symbol.from("SPECIFIC");
  public final static Symbol GENERIC_SYM = Symbol.from("GENERIC");
  public final static Symbol APPOSITIVE_SYM = Symbol.from("APPOSITIVE");
  public final static Symbol APPOSITIVE_CHILD_SYM = Symbol.from("APPOSITIVE_CHILD");
  public final static Symbol NAMED_APPOSITIVE_CHILD_SYM = Symbol.from("NAMED_APPOSITIVE_CHILD");
  public final static Symbol BLOCK_FT_SYM = Symbol.from("BLOCK_FALL_THROUGH");
  public final static Symbol DOC_ETYPE_FREQ_SYM = Symbol.from("doc_etype_freq");
  public final static Symbol SENT_ETYPE_FREQ_SYM = Symbol.from("sent_etype_freq");
  public final static Symbol BROWN_CLUSTER_SYM = Symbol.from("brown_cluster");
  public final static Symbol EQUAL_SYM = Symbol.from("==");
  public final static Symbol NOT_EQUAL_SYM = Symbol.from("!=");
  public final static Symbol LESS_THAN_SYM = Symbol.from("<");
  public final static Symbol LESS_THAN_EQUAL_SYM = Symbol.from("<=");
  public final static Symbol GREATER_THAN_SYM = Symbol.from(">");
  public final static Symbol GREATER_THAN_EQUAL_SYM = Symbol.from(">=");
  public final static Symbol PROP_DEF_SYM = Symbol.from("prop-def");
  public final static Symbol ARG_OF_PROP_SYM = Symbol.from("arg-of-prop");
  public final static Symbol ROLE_SYM = Symbol.from("role");

  //Value Pattern
  public final static Symbol VALUE_TYPE_SYM = Symbol.from("type");
  public final static Symbol ACTIVITY_DATE_SYM = Symbol.from("activity-date");
  public final static Symbol SPECIFIC_DATE_SYM = Symbol.from("SPECIFIC-DATE");
  public final static Symbol RECENT_DATE_SYM = Symbol.from("RECENT-DATE");
  public final static Symbol FUTURE_DATE_SYM = Symbol.from("FUTURE-DATE");
  public final static Symbol VALUE_MENTION_LABEL_SYM = Symbol.from("valuementionlabel");
  public final static Symbol DOC_VTYPE_FREQ_SYM = Symbol.from("doc_vtype_freq");
  public final static Symbol SENT_VTYPE_FREQ_SYM = Symbol.from("sent_vtype_freq");
  public final static Symbol IN_RANGE_SYM = Symbol.from("IN_RANGE");
  public final static Symbol OUT_OF_RANGE_SYM = Symbol.from("OUT_OF_RANGE");
  public final static Symbol NOT_SPECIFIC_SYM = Symbol.from("NOT_SPECIFIC");
  public final static Symbol TOO_BROAD_SYM = Symbol.from("TOO_BROAD");

  //Prop Pattern
  public final static Symbol BLOCK_ARGS_SYM = Symbol.from("block_args");
  public final static Symbol ARGS_SYM = Symbol.from("args");
  public final static Symbol OPT_ARGS_SYM = Symbol.from("opt_args");
  public final static Symbol ADJECTIVE_SYM = Symbol.from("adj");
  public final static Symbol BLOCK_ADJECTIVE_SYM = Symbol.from("block_adj");
  public final static Symbol ADVERB_PARTICLE_SYM = Symbol.from("adverb_or_particle");
  public final static Symbol BLOCK_ADVERB_PARTICLE_SYM = Symbol.from("block_adv_part");
  public final static Symbol PREDICATE_SYM = Symbol.from("predicate");
  public final static Symbol ALIGNED_PREDICATE_SYM = Symbol.from("aligned_predicate");
  public final static Symbol BLOCK_PREDICATE_SYM = Symbol.from("block_predicate");
  public final static Symbol PRED_TYPE_SYM = Symbol.from("predicate_type");
  public final static Symbol DEFINITE_SYM = Symbol.from("DEFINITE");
  public final static Symbol NEGATIVE_SYM = Symbol.from("NEGATIVE");
  public final static Symbol MATCH_ALL_ARGS_SYM = Symbol.from("MATCH_ALL_ARGS");
  public final static Symbol STEM_PREDICATE_SYM = Symbol.from("STEM_PREDICATE");
  public final static Symbol PARTICLE_SYM = Symbol.from("particle");
  public final static Symbol PROP_MODIFIER_SYM = Symbol.from("propmod");
  public final static Symbol ONE_TO_ONE_SYM = Symbol.from("ONE_TO_ONE");
  public final static Symbol MANY_TO_MANY_SYM = Symbol.from("MANY_TO_MANY");
  public final static Symbol BLOCK_NEGATION_SYM = Symbol.from("block_negation");
  public final static Symbol MODAL_SYM = Symbol.from("modal");
  public final static Symbol BLOCK_MODAL_SYM = Symbol.from("block_modal");

  //Regex Pattern
  public final static Symbol RE_SYM = Symbol.from("re");
  public final static Symbol DONT_ALLOW_HEADS_SYM = Symbol.from("DONT_ALLOW_HEADS");
  public final static Symbol MATCH_WHOLE_EXTENT_SYM = Symbol.from("MATCH_FULL_EXTENT");
  public final static Symbol TOP_MENTIONS_ONLY_SYM = Symbol.from("TOP_MENTIONS_ONLY");
  public final static Symbol DONT_ADD_SPACES_SYM = Symbol.from("DONT_ADD_SPACES");
  public final static Symbol NONE_SYM = Symbol.from("NONE");

  //Text Pattern
  public final static Symbol STRING_SYM = Symbol.from("string");
  public final static Symbol RAW_TEXT_SYM = Symbol.from("RAW_TEXT");
  //public final static Symbol DONT_ADD_SPACES_SYM = Symbol.from("DONT_ADD_SPACES");

  //Argument Pattern
  //public final static Symbol ARGUMENT_SYM = Symbol.from("argument");
  //public final static Symbol ROLE_SYM = Symbol.from("role");
  public final static Symbol OPTIONAL_SYM = Symbol.from("OPT");
  public final static Symbol FALL_THROUGH_SYM = Symbol.from("allow_fall_through");

  //Combination Pattern/Intersection Pattern/Union Pattern
  public final static Symbol MEMBERS_SYM = Symbol.from("members");
  public final static Symbol GREEDY_SYM = Symbol.from("GREEDY");


  private final Map<Symbol, Pattern> referencePatterns;
  private final Map<Symbol, Set<Symbol>> wordSets;

  public PatternFactory(Map<Symbol, Pattern> referencePatterns, Map<Symbol, Set<Symbol>> wordSets) {
    this.referencePatterns = referencePatterns;
    this.wordSets = wordSets;
  }

  public PatternFactory() {
    this(new HashMap<Symbol, Pattern>(), new HashMap<Symbol, Set<Symbol>>());
  }


  public Pattern fromSexp(Sexp sexp) {
    if (!(sexp instanceof SexpList)) {
      throw new PatternSexpParsingException("Pattern sexp is not a list", sexp);
    }

    SexpList children = (SexpList) sexp;
    if (children.size() <= 1) {
      throw new PatternSexpParsingException("Pattern sexp has too few elements", sexp);
    }

    Sexp typeSexp = children.get(0);
    if (!(typeSexp instanceof SexpAtom)) {
      throw new PatternSexpParsingException("Could not get pattern sexp type from " +
          typeSexp.toString(), sexp);
    }
    Symbol type = ((SexpAtom) typeSexp).getValue();

    return fromType(type, children.subList(1, children.size()));

  }

  private Pattern fromType(Symbol type, List<Sexp> sexp) {
    if (type.equals(MENTION_SYM)) {
      return buildPattern(new MentionPattern.Builder(), sexp, mentionPatternBUH);
    } else if (type.equals(VPROP_SYM)) {
      return buildPattern(new PropPattern.Builder(PredicateType.VERB), sexp, propPatternBUH);
    } else if (type.equals(NPROP_SYM)) {
      return buildPattern(new PropPattern.Builder(PredicateType.NOUN), sexp, propPatternBUH);
    } else if (type.equals(MPROP_SYM)) {
      return buildPattern(new PropPattern.Builder(PredicateType.MODIFIER), sexp, propPatternBUH);
    } else if (type.equals(CPROP_SYM)) {
      return buildPattern(new PropPattern.Builder(PredicateType.COMP), sexp, propPatternBUH);
    } else if (type.equals(SPROP_SYM)) {
      return buildPattern(new PropPattern.Builder(PredicateType.SET), sexp, propPatternBUH);
    } else if (type.equals(ANYPROP_SYM)) {
      return buildPattern(new PropPattern.Builder(null), sexp, propPatternBUH);
    } else if (type.equals(REGEX_SYM)) {
      return buildPattern(new RegexPattern.Builder(), sexp, regexPatternBUH);
    } else if (type.equals(TEXT_SYM)) {
      return buildPattern(new TextPattern.Builder(), sexp, textPatternBUH);
    } else if (type.equals(ARGUMENT_SYM)) {
      return buildPattern(new ArgumentPattern.Builder(), sexp, argumentPatternBUH);
    } else if (type.equals(ALL_OF_SYM)) {
      return buildPattern(new CombinationPattern.Builder(CombinationPattern.CombinationType.ALL_OF),
          sexp, combinationPatternBUH);
    } else if (type.equals(ANY_OF_SYM)) {
      return buildPattern(new CombinationPattern.Builder(CombinationPattern.CombinationType.ANY_OF),
          sexp, combinationPatternBUH);
    } else if (type.equals(NONE_OF_SYM)) {
      return buildPattern(
          new CombinationPattern.Builder(CombinationPattern.CombinationType.NONE_OF),
          sexp, combinationPatternBUH);
    } else if (type.equals(INTERSECTION_SYM)) {
      return buildPattern(new IntersectionPattern.Builder(), sexp, intersectionPatternBUH);
    } else if (type.equals(UNION_SYM)) {
      return buildPattern(new UnionPattern.Builder(), sexp, unionPatternBUH);
      //} else if (type.equals(QUOTATION_SYM)) {
      //} else if (type.equals(PARSE_NODE_SYM)) {
      //} else if (type.equals(DOC_SYM) {
      //} else if (type.equals(EVENT_SYM)) {
      //} else if (type.equals(NEGATION_SYM)) {
      //} else if (type.equals(RELATION_SYM)) {
    } else if (type.equals(VALUE_SYM)) {
      return buildPattern(new ValueMentionPattern.Builder(), sexp, valueMentionPatternBUH);
    } else {
      throw new PatternSexpParsingException("Unrecognized Pattern type " + type.toString());
    }
  }

  private <T extends Pattern.Builder> Pattern buildPattern(
      T builder, List<Sexp> properties, BuilderUpdateHandler<T> updateHandler) {

    for (Sexp s : properties) {
      updateHandler.updateBuilder(builder, SexpUtils.getSexpType(s), SexpUtils.getSexpArgs(s));
    }
    return builder.build();
  }


  //CONVERSION FUNCTIONS
  private final Function<Sexp, PatternReturn> convertSexpToPatternReturn =
      new Function<Sexp, PatternReturn>() {
        @Override
        public PatternReturn apply(Sexp sexp) {
          if (sexp instanceof SexpAtom) {
            return new LabelPatternReturn(((SexpAtom) sexp).getValue());
          } else {
            MapPatternReturn.Builder builder = new MapPatternReturn.Builder();
            Map<Symbol, Sexp> argMap = SexpUtils.getSexpArgMap(sexp);
            for (Symbol key : argMap.keySet()) {
              builder.withReturnAdd(key.toString(),
                  SexpUtils.getCheckAtom(argMap.get(key)).toString());
            }
            return builder.build();
          }
        }
      };

  private final Function<Sexp, EntityType> convertSexpToEntityType =
      new Function<Sexp, EntityType>() {
        @Override
        public EntityType apply(Sexp sexp) {
          Symbol symbol = SexpUtils.getCheckAtom(sexp);
          return EntityType.of(symbol);
        }
      };

  private final Function<Sexp, ValueType> convertSexpToValueType =
      new Function<Sexp, ValueType>() {
        @Override
        public ValueType apply(Sexp sexp) {
          return ValueType.parseDottedPair(SexpUtils.getCheckAtom(sexp).asString());
        }
      };

  private final Function<Sexp, EntitySubtype> convertSexpToEntitySubtype =
      new Function<Sexp, EntitySubtype>() {
        @Override
        public EntitySubtype apply(Sexp sexp) {
          return EntitySubtype.of(SexpUtils.getCheckAtom(sexp));
        }
      };

  private final Function<Sexp, Mention.Type> convertSexpToMentionType =
      new Function<Sexp, Mention.Type>() {
        @Override
        public Mention.Type apply(Sexp sexp) {
          Symbol symbol = SexpUtils.getCheckAtom(sexp);
          return Mention.Type.valueOf(symbol.toString());
        }
      };

  private final Function<Sexp, Pattern> convertSexpToPattern =
      new Function<Sexp, Pattern>() {
        @Override
        public Pattern apply(Sexp sexp) {
          if (sexp instanceof SexpAtom) {
            //must be a shortcut
            Symbol shortcut = ((SexpAtom) sexp).getValue();
            if (!referencePatterns.containsKey(shortcut)) {
              throw new PatternSexpParsingException(
                  "Pattern shortcut " + shortcut.toString() + " not found.", sexp);
            }
            return referencePatterns.get(shortcut);
          } else {
            return fromSexp(sexp);
          }
        }
      };

  private final Function<Sexp, ArgumentPattern> convertSexpToArgPattern =
      new Function<Sexp, ArgumentPattern>() {
        @Override
        public ArgumentPattern apply(Sexp sexp) {
          if (sexp instanceof SexpAtom) {
            //must be a shortcut
            Symbol shortcut = ((SexpAtom) sexp).getValue();
            if (!referencePatterns.containsKey(shortcut)) {
              throw new PatternSexpParsingException(
                  "Pattern shortcut " + shortcut.toString() + " not found.", sexp);
            }
            return (ArgumentPattern) referencePatterns.get(shortcut);
          } else {
            return (ArgumentPattern) fromSexp(sexp);
          }
        }
      };


  /**
   * This is a function that will pull out wildcard symbols (ending with *) or non-wildcard symbols
   * from a sexp list. This is needed because of functionality that separates wildcards into
   * separate lists.
   *
   * It also uses wordsets
   *
   * I put it here because it seems specific to the quirks of patterns
   *
   * @param wildcard whether to get the wildcard or non-wildcard symbols
   */
  private List<Symbol> getCheckSymListWild(Sexp s, boolean wildcard) {
    SexpList slist = SexpUtils.forceList(s);
    List<Symbol> result = new ArrayList<Symbol>();
    for (Sexp childSexp : slist) {
      Symbol symbol = SexpUtils.getCheckAtom(childSexp);

      List<Symbol> atoms = new ArrayList<Symbol>();
      if (wordSets.containsKey(symbol)) {
        atoms.addAll(wordSets.get(symbol));
      } else {
        atoms.add(symbol);
      }

      for (Symbol atom : atoms) {
        if (wildcard && atom.toString().endsWith("*")) {
          String substring = atom.toString().substring(0, atom.toString().length() - 1);
          result.add(Symbol.from(substring));
        } else if (!wildcard && !atom.toString().endsWith("*")) {
          result.add(atom);
        }
      }
    }
    return result;
  }

  public Set<Symbol> getCheckSymSetWild(Sexp s, boolean wildcard) {
    return new HashSet<Symbol>(getCheckSymListWild(s, wildcard));
  }

  //BUILDER UPDATE HANDLERS ----------------------------------------------

  /**
   * Builder Update Handlers handle translation of sexp to properties handed to the builders.
   *
   * @author mshafir
   */
  private interface BuilderUpdateHandler<T> {

    public boolean updateBuilder(T builder, Symbol type, Sexp element);
  }


  private class PatternBuilderUpdateHandler
      implements BuilderUpdateHandler<Pattern.Builder> {

    @Override
    public boolean updateBuilder(Pattern.Builder builder, Symbol type, Sexp element) {
      if (type.equals(SCORE_SYM)) {
        builder.withScore(SexpUtils.getCheckFloatAtom(element));
      } else if (type.equals(ID_SYM)) {
        builder.withId(SexpUtils.getCheckAtom(element));
      } else if (type.equals(SHORTCUT_SYM)) {
        builder.withShortcut(SexpUtils.getCheckAtom(element));
      } else if (type.equals(RETURN_SYM)) {
        builder.withPatternReturn(convertSexpToPatternReturn.apply(element));
      } else if (type.equals(SCORE_GROUP_SYM)) {
        builder.withScoreGroup(SexpUtils.getCheckIntAtom(element));
      } else if (type.equals(SCORE_FN_SYM)) {
        builder.withScoreFunction(SexpUtils.getCheckAtom(element));
      } else if (type.equals(TOP_LEVEL_RETURN_SYM)) {
        builder.withTopLevelReturn(true);
      } else {
        return false;
      }
      return true;
    }
  }

  private final PatternBuilderUpdateHandler patternBUH =
      new PatternBuilderUpdateHandler();


  private static class LanguageVariantPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<LanguageVariantSwitchingPattern.Builder> {

    @Override
    public boolean updateBuilder(LanguageVariantSwitchingPattern.Builder builder, Symbol type,
        Sexp element) {
      if (type.equals(LANGUAGE_SYM)) {
        builder.withLanguage(SexpUtils.getCheckAtom(element));
      } else if (type.equals(VARIATION_SYM)) {
        builder.withVariant(SexpUtils.getCheckAtom(element));
      } else {
        return false;
      }
      return true;
    }
  }

  private final LanguageVariantPatternBuilderUpdateHandler languageVariantPatternBUH =
      new LanguageVariantPatternBuilderUpdateHandler();


  private class MentionPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<MentionPattern.Builder> {

    @Override
    public boolean updateBuilder(
        com.bbn.serif.patterns.MentionPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (languageVariantPatternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(FOCUS_SYM)) {
        builder.withIsFocus(true);
      } else if (type.equals(SPECIFIC_SYM)) {
        builder.withIsSpecific(true);
      } else if (type.equals(GENERIC_SYM)) {
        builder.withIsGeneric(true);
      } else if (type.equals(APPOSITIVE_SYM)) {
        builder.withIsAppositive(true);
      } else if (type.equals(APPOSITIVE_CHILD_SYM)) {
        builder.withIsAppositiveChild(true);
      } else if (type.equals(NAMED_APPOSITIVE_CHILD_SYM)) {
        builder.withIsNamedAppositive(true);
      } else if (type.equals(BLOCK_FT_SYM)) {
        builder.withBlockFallThrough(true);
      } else if (type.equals(ACETYPE_SYM)) {
        builder.withAceTypes(SexpUtils.getCheckList(element, convertSexpToEntityType));
      } else if (type.equals(ACESUBTYPE_SYM)) {
        builder.withAceSubtypes(SexpUtils.getCheckList(element, convertSexpToEntitySubtype));
      } else if (type.equals(MENTIONTYPE_SYM)) {
        builder.withMentionTypes(SexpUtils.getCheckSet(element, convertSexpToMentionType));
      } else if (type.equals(DOC_ETYPE_FREQ_SYM) || type.equals(SENT_ETYPE_FREQ_SYM)) {
        SexpList slist = SexpUtils.forceList(element);
        if (slist.size() != 2) {
          throw new PatternSexpParsingException(
              "Invalid etype-freq syntax", element);
        }
        builder.withComparisonConstraintsAdd(new ComparisonConstraint(
            type, SexpUtils.getCheckAtom(slist.get(0)), SexpUtils.getCheckIntAtom(slist.get(1))));
      } else if (type.equals(MIN_ENTITYLEVEL_SYM)) {
        Symbol nameOrDesc = SexpUtils.getCheckAtom(element);
        if (nameOrDesc.equals(NAME_SYM)) {
          builder.withRequiresName(true);
        } else if (nameOrDesc.equals(DESC_SYM)) {
          builder.withRequiresNameOrDesc(true);
        } else {
          throw new PatternSexpParsingException(
              "minenentitylevel must be NAME or DESC", element);
        }
      } else if (type.equals(ENTITY_LABEL_SYM)) {
        builder.withEntityLabels(SexpUtils.getCheckSymList(element));
      } else if (type.equals(BLOCK_SYM)) {
        builder.withBlockingEntityLabels(SexpUtils.getCheckSymList(element));
      } else if (type.equals(HEADWORD_SYM)) {
        builder.withHeadwords(SexpUtils.getCheckSymSetWild(element, false));
        builder.withHeadwordPrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(BLOCK_ACETYPE_SYM)) {
        builder.withBlockedAceTypes(SexpUtils.getCheckList(element, convertSexpToEntityType));
      } else if (type.equals(BLOCK_HEADWORD_SYM)) {
        builder.withBlockedHeadwords(SexpUtils.getCheckSymSetWild(element, false));
        builder.withBlockedHeadwordPrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(REGEX_SYM) || type.equals(HEAD_REGEX_SYM)) {
        Sexp subelement = SexpUtils.forceList(element).get(0);
        builder.withRegexPattern(convertSexpToPattern.apply(subelement));
        if (type.equals(HEAD_REGEX_SYM)) {
          builder.withHeadRegex(true);
        }
      } else if (type.equals(PROP_DEF_SYM)) {
        Sexp subelement = SexpUtils.forceList(element).get(0);
        builder.withPropDefPattern(convertSexpToPattern.apply(subelement));
      } else if (type.equals(ARG_OF_PROP_SYM)) {
        SexpList slist = SexpUtils.forceList(element);
        Pattern p;
        Set<Symbol> roles;
        if (slist.size() == 2) {
          p = convertSexpToPattern.apply(slist.get(1));
          roles = new HashSet<Symbol>();
        } else if (slist.size() == 3) {
          roles = SexpUtils.getCheckSymSet(slist.get(1), wordSets);
          p = convertSexpToPattern.apply(slist.get(2));
        } else {
          throw new PatternSexpParsingException(
              "Invalid arg-of-prop syntax: (arg-of-prop [(role ...)] (prop ...))", element);
        }
        builder.withArgOfPropConstraintsAdd(new
            MentionPattern.ArgOfPropConstraint(roles, p));
      } else if (type.equals(BROWN_CLUSTER_SYM)) {
        Symbol brownClusterConstraintSym = SexpUtils.getCheckAtom(element);
        String clusterString = brownClusterConstraintSym.toString();

        builder.withBrownClusterConstraint(brownClusterConstraintSym);
        if (brownClusterConstraintSym.toString().endsWith("*")) {
          builder.withBrownClusterPrefix(Long.parseLong(
              clusterString.substring(0, clusterString.length() - 1)));
        } else {
          builder.withBrownCluster(Long.parseLong(clusterString));
        }
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse mention pattern property " + type.toString(), element);
      }
      return true;
    }

  }

  private final MentionPatternBuilderUpdateHandler mentionPatternBUH =
      new MentionPatternBuilderUpdateHandler();


  private class ValueMentionPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<ValueMentionPattern.Builder> {

    @Override
    public boolean updateBuilder(
        com.bbn.serif.patterns.ValueMentionPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(SPECIFIC_DATE_SYM)) {
        builder.withMustBeSpecificDate(true);
      } else if (type.equals(RECENT_DATE_SYM)) {
        builder.withMustBeRecentDate(true);
      } else if (type.equals(FUTURE_DATE_SYM)) {
        builder.withMustBeFutureDate(true);
      } else if (type.equals(REGEX_SYM)) {
        Sexp subelement = SexpUtils.forceList(element).get(0);
        builder.withRegexPattern(convertSexpToPattern.apply(subelement));
      } else if (type.equals(VALUE_TYPE_SYM)) {
        builder.withValueTypes(SexpUtils.getCheckList(element, convertSexpToValueType));
      } else if (type.equals(DOC_VTYPE_FREQ_SYM) || type.equals(SENT_VTYPE_FREQ_SYM)) {
        SexpList slist = SexpUtils.forceList(element);
        if (slist.size() != 2) {
          throw new PatternSexpParsingException(
              "Invalid vtype-freq syntax", element);
        }
        builder.withComparisonConstraintsAdd(new ComparisonConstraint(
            type, SexpUtils.getCheckAtom(slist.get(0)), SexpUtils.getCheckIntAtom(slist.get(1))));
      } else if (type.equals(ACTIVITY_DATE_SYM)) {
        Symbol nameOrDesc = SexpUtils.getCheckAtom(element);
        if (nameOrDesc.equals(IN_RANGE_SYM)) {
          builder.withActivityDateStatus(ValueMentionPattern.DateStatus.IN_RANGE);
        } else if (nameOrDesc.equals(OUT_OF_RANGE_SYM)) {
          builder.withActivityDateStatus(ValueMentionPattern.DateStatus.OUT_OF_RANGE);
        } else if (nameOrDesc.equals(TOO_BROAD_SYM)) {
          builder.withActivityDateStatus(ValueMentionPattern.DateStatus.TOO_BROAD);
        } else if (nameOrDesc.equals(NOT_SPECIFIC_SYM)) {
          builder.withActivityDateStatus(ValueMentionPattern.DateStatus.NOT_SPECIFIC);
        } else {
          throw new PatternSexpParsingException(
              "activity-date must be IN_RANGE, OUT_OF_RANGE, TOO_BROAD, or NOT_SPECIFIC", element);
        }
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse mention pattern property " + type.toString(), element);
      }
      return true;
    }
  }

  private final ValueMentionPatternBuilderUpdateHandler valueMentionPatternBUH =
      new ValueMentionPatternBuilderUpdateHandler();


  private class PropPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<PropPattern.Builder> {

    @Override
    public boolean updateBuilder(PropPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (languageVariantPatternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(NEGATIVE_SYM)) {
        builder.withPsmManuallyInitialized(true);
      } else if (type.equals(ONE_TO_ONE_SYM)) {
        builder.withRequireOneToOneArgumentMapping(true);
      } else if (type.equals(MANY_TO_MANY_SYM)) {
        builder.withAllowManyToManyMapping(true);
      } else if (type.equals(MATCH_ALL_ARGS_SYM)) {
        builder.withRequireAllArgumentsToMatchSomePattern(true);
      } else if (type.equals(STEM_PREDICATE_SYM)) {
        builder.withStemPredicate(true);
      } else if (type.equals(PREDICATE_SYM)) {
        builder.withPredicates(SexpUtils.getCheckSymSetWild(element, false));
        builder.withPredicatePrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(ALIGNED_PREDICATE_SYM)) {
        builder.withAlignedPredicates(SexpUtils.getCheckSymSet(element));
      } else if (type.equals(BLOCK_PREDICATE_SYM)) {
        builder.withBlockedPredicates(SexpUtils.getCheckSymSetWild(element, false));
        builder.withBlockedPredicatePrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(PARTICLE_SYM)) {
        builder.withParticles(SexpUtils.getCheckSymSet(element, wordSets));
      } else if (type.equals(ADJECTIVE_SYM)) {
        builder.withAdjectives(SexpUtils.getCheckSymSet(element, wordSets));
      } else if (type.equals(BLOCK_ADJECTIVE_SYM)) {
        builder.withBlockedAdjectives(SexpUtils.getCheckSymSet(element, wordSets));
      } else if (type.equals(BLOCK_ADVERB_PARTICLE_SYM)) {
        builder.withBlockedAdverbsOrParticles(SexpUtils.getCheckSymSet(element, wordSets));
      } else if (type.equals(ADVERB_PARTICLE_SYM)) {
        builder.withBlockedAdverbsOrParticles(SexpUtils.getCheckSymSet(element, wordSets));
      } else if (type.equals(NEGATION_SYM)) {
        builder.withNegations(SexpUtils.getCheckSymSetWild(element, false));
        builder.withNegationPrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(BLOCK_NEGATION_SYM)) {
        builder.withBlockedNegations(SexpUtils.getCheckSymSetWild(element, false));
        builder.withBlockedNegationPrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(MODAL_SYM)) {
        builder.withModals(SexpUtils.getCheckSymSetWild(element, false));
        builder.withModalPrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(BLOCK_MODAL_SYM)) {
        builder.withBlockedModals(SexpUtils.getCheckSymSetWild(element, false));
        builder.withBlockedModalPrefixes(SexpUtils.getCheckSymSetWild(element, true));
      } else if (type.equals(REGEX_SYM)) {
        Sexp subelement = SexpUtils.forceList(element).get(0);
        builder.withRegexPattern(convertSexpToPattern.apply(subelement));
      } else if (type.equals(BLOCK_ARGS_SYM)) {
        builder.withBlockedArgs(SexpUtils.getCheckList(element, convertSexpToArgPattern));
      } else if (type.equals(ARGS_SYM)) {
        builder.withArgs(SexpUtils.getCheckList(element, convertSexpToArgPattern));
      } else if (type.equals(OPT_ARGS_SYM)) {
        builder.withOptArgs(SexpUtils.getCheckList(element, convertSexpToArgPattern));
      } else if (type.equals(PROP_MODIFIER_SYM)) {
        SexpList slist = SexpUtils.forceList(element);
        if (slist.size() == 3) {
          builder.withPropModifierRoles(SexpUtils.getCheckSymSet(slist.get(1), wordSets))
              .withPropModifierPattern(convertSexpToPattern.apply(slist.get(2)));
        } else {
          throw new PatternSexpParsingException(
              "Invalid propmod syntax: (propMod (role ...) PATTERN)", element);
        }
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse prop pattern property " + type.toString(), element);
      }
      return true;
    }
  }

  private final PropPatternBuilderUpdateHandler propPatternBUH =
      new PropPatternBuilderUpdateHandler();


  private class RegexPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<RegexPattern.Builder> {

    @Override
    public boolean updateBuilder(RegexPattern.Builder builder,
        Symbol type, Sexp element) {
      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (languageVariantPatternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(DONT_ALLOW_HEADS_SYM)) {
        builder.withAllowHeads(false);
      } else if (type.equals(TOP_MENTIONS_ONLY_SYM)) {
        builder.withTopMentionsOnly(true);
      } else if (type.equals(MATCH_WHOLE_EXTENT_SYM)) {
        builder.withMatchWholeExtent(true);
      } else if (type.equals(DONT_ADD_SPACES_SYM)) {
        builder.withAddSpaces(false);
      } else if (type.equals(RE_SYM)) {
        builder.withSubpatterns(SexpUtils.getCheckList(element, convertSexpToPattern));
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse regex pattern property " + type.toString(), element);
      }
      return true;
    }

  }

  private final RegexPatternBuilderUpdateHandler regexPatternBUH =
      new RegexPatternBuilderUpdateHandler();


  private class TextPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<TextPattern.Builder> {

    @Override
    public boolean updateBuilder(TextPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(RAW_TEXT_SYM)) {
        builder.withRawText(true);
      } else if (type.equals(DONT_ADD_SPACES_SYM)) {
        builder.withAddSpaces(false);
      } else if (type.equals(STRING_SYM)) {
        StringBuilder text = new StringBuilder();
        for (Sexp s : SexpUtils.forceList(element)) {
          //TODO: word lists will need to be handled
          String item = SexpUtils.getCheckAtom(s).toString();
          text.append(item);
        }
        builder.withText(text.toString());
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse text pattern property " + type.toString(), element);
      }
      return true;
    }

  }

  private final TextPatternBuilderUpdateHandler textPatternBUH =
      new TextPatternBuilderUpdateHandler();


  private class ArgumentPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<ArgumentPattern.Builder> {

    @Override
    public boolean updateBuilder(ArgumentPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(OPTIONAL_SYM)) {
        builder.withOptional(true);
      } else if (type.equals(FALL_THROUGH_SYM)) {
        builder.withFallThroughRoles(SexpUtils.getCheckSymList(element, wordSets));
      } else if (type.equals(ROLE_SYM)) {
        builder.withRoles(SexpUtils.getCheckSymList(element, wordSets));
      } else if (referencePatterns.containsKey(type)) {
          builder.withPattern(referencePatterns.get(type));
      } else {
          builder.withPattern(fromType(type, SexpUtils.forceList(element)));
      }
      return true;
    }

  }

  private final ArgumentPatternBuilderUpdateHandler argumentPatternBUH =
      new ArgumentPatternBuilderUpdateHandler();


  private class CombinationPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<CombinationPattern.Builder> {

    @Override
    public boolean updateBuilder(CombinationPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(GREEDY_SYM)) {
        builder.withIsGreedy(true);
      } else if (type.equals(MEMBERS_SYM)) {
        builder.withPatternList(SexpUtils.getCheckList(element, convertSexpToPattern));
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse combination pattern property " + type.toString(), element);
      }
      return true;
    }

  }

  private final CombinationPatternBuilderUpdateHandler combinationPatternBUH =
      new CombinationPatternBuilderUpdateHandler();


  private class IntersectionPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<IntersectionPattern.Builder> {

    @Override
    public boolean updateBuilder(IntersectionPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }
      if (languageVariantPatternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(MEMBERS_SYM)) {
        builder.withPatternList(SexpUtils.getCheckList(element, convertSexpToPattern));
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse intersection pattern property", element);
      }
      return true;
    }

  }

  private final IntersectionPatternBuilderUpdateHandler intersectionPatternBUH =
      new IntersectionPatternBuilderUpdateHandler();


  private class UnionPatternBuilderUpdateHandler
      implements BuilderUpdateHandler<UnionPattern.Builder> {

    @Override
    public boolean updateBuilder(UnionPattern.Builder builder,
        Symbol type, Sexp element) {

      if (patternBUH.updateBuilder(builder, type, element)) {
        return true;
      }
      if (languageVariantPatternBUH.updateBuilder(builder, type, element)) {
        return true;
      }

      if (type.equals(GREEDY_SYM)) {
        builder.withIsGreedy(true);
      } else if (type.equals(MEMBERS_SYM)) {
        builder.withPatternList(SexpUtils.getCheckList(element, convertSexpToPattern));
      } else {
        throw new PatternSexpParsingException(
            "Failed to parse union pattern property " + type.toString(), element);
      }
      return true;
    }

  }

  private final UnionPatternBuilderUpdateHandler unionPatternBUH =
      new UnionPatternBuilderUpdateHandler();
}
