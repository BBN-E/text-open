package com.bbn.serif.patterns2;

import com.bbn.bue.common.SExpression;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.types.EntitySubtype;
import com.bbn.serif.types.EntityType;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.skip;

/**
 * Parses an S-expression in Brandy Pattern Language format into a {@link Pattern}
 */
@Beta
public final class PatternSExpressionParser {
  private PatternSExpressionParser() {}

  public static PatternSExpressionParser create() {
    return new PatternSExpressionParser();
  }

  public Pattern parseFrom(SExpression sExpression)  {
    return new PatternSExpressionParsing().parse(sExpression);
  }

  public static final class PatternSExpressionParsing {
    private final Map<Symbol, Pattern> shortcuts = Maps.newHashMap();
    private final Map<Symbol, WordSet> wordsets = Maps.newHashMap();

    private PatternSExpressionParsing() {}

    public Pattern parse(SExpression sexp)  {
      if (sexp.atom().isPresent()) {
        return lookupShortcutRequired(sexp.atom().get());
      }

      parseAssert(sexp.children().size() > 0,
          "Cannot parse an empty S-expression as a pattern", sexp);
      final SExpression firstChild = sexp.children().get(0);
      parseAssert(firstChild.atom().isPresent(),
          "Cannot parse a S-expression as a pattern unless first element is pattern type.", sexp);

      switch (firstChild.atom().get().asString()) {
        case "regex":
          return parseRegex(sexp);
        case "text":
          return parseText(sexp);
        case "mention":
          return parseMention(sexp);
        default:
          throw new ParseException("Don't know how to parse S-expressions of type "
              + firstChild.atom().get().asString());
      }
    }

    private Pattern lookupShortcutRequired(Symbol shortcut) throws ParseException {
      final Pattern ret = shortcuts.get(shortcut);
      if (ret != null) {
        return ret;
      } else {
        throw new ParseException("Cannot find shortcut " + shortcut + ". Known shortcuts are "
            + SymbolUtils.byStringOrdering().immutableSortedCopy(shortcuts.keySet()));
      }
    }

    private Optional<WordSet> lookupWordset(Symbol name) {
      return Optional.fromNullable(wordsets.get(name));
    }

    private RegexPattern parseRegex(SExpression sexp) throws ParseException {
      final RegexPattern.Builder ret = new RegexPattern.Builder();
      // we skip the type symbol
      for (SExpression child : skip(sexp.children(), 1)) {
        if (child.atom().isPresent()) {
          switch (child.atom().get().asString()) {
            case "DONT_ALLOW_HEADS":
              ret.allowHeads(false);
              break;
            case "MATCH_FULL_EXTENT":
              ret.matchWholeExtent(true);
              break;
            case "TOP_MENTIONS_ONLY":
              ret.topMentionsOnly(true);
              break;
            case "DONT_ADD_SPACES":
              ret.addSpaces(false);
              break;
            default:
              throw new ParseException("Unknown regex property " + child.atom().get().asString());
          }
        } else {
          if (!child.children().isEmpty() && child.children().get(0).isAtom("re")) {
            // skip "re" type atom we checked for above
            for (final SExpression grandchild : skip(child.children(), 1)) {
              ret.addSubpatterns(parse(grandchild));
            }
          } else {
            throw new ParseException("Unknown regex property " + child);
          }
        }
      }
      return ret.build();
    }

    private static final String RAW_TEXT = "RAW_TEXT";
    private static final String DONT_ADD_SPACES = "DONT_ADD_SPACES";

    private Pattern parseText(SExpression sexp) throws ParseException {
      final TextPattern.Builder ret = new TextPattern.Builder();

      boolean seenText = false;
      boolean addSpaces = true;

      final StringBuilder text = new StringBuilder();

      // skip type element
      for (final SExpression kid : skip(sexp.children(), 1)) {
        if (kid.atom().isPresent()) {
          switch (kid.atom().get().asString()) {
            case RAW_TEXT:
              if (seenText) {
                throw new ParseException(RAW_TEXT + " must precede string constraint");
              }
              ret.rawText(true);
              break;
            case DONT_ADD_SPACES:
              if (seenText) {
                throw new ParseException(DONT_ADD_SPACES + " must precede string constraint");
              }
              addSpaces = false;
              ret.addSpaces(false);
              break;
            default:
              throw new ParseException("Unknown TextPattern property " + kid.atom().get());
          }
        } else {
          if (!kid.children().isEmpty() && kid.children().get(0).isAtom("string")) {
            seenText = true;
            parseAssert(kid.children().size() >= 2,
                "string constraint must be followed by at least 1 atomic S-expression", kid);
            // skip type atom we just checked for
            for (int grandKidIdx = 1; grandKidIdx < kid.children().size(); ++grandKidIdx) {
              final SExpression grandkid = kid.children().get(grandKidIdx);
              // we use an old-fashioned for loop so we can easily check if we are the last kid
              final boolean isLastGrandkid = grandKidIdx == kid.children().size() - 1;

              parseAssert(grandkid.atom().isPresent(),
                  "string constraint may contain only atomic S-expreesions", kid);
              @SuppressWarnings("OptionalGetWithoutIsPresent")
              final Optional<WordSet> wordSet = lookupWordset(grandkid.atom().get());
              if (wordSet.isPresent()) {
                text.append(wordSet.get().asRegularExpression());
                if (!isLastGrandkid && addSpaces) {
                  text.append(' ');
                }
              } else {
                //noinspection OptionalGetWithoutIsPresent
                text.append(grandkid.atom().get().asString());
                if (!isLastGrandkid && addSpaces) {
                  text.append(' ');
                }
              }
            }
          }
        }
      }

      ret.text(text.toString());
      return ret.build();
    }

    private Pattern parseMention(SExpression sExpression) {
      checkArgument(sExpression.children().size() >= 1
          && sExpression.children().get(0).isAtom("mention"));

      final MentionPattern.Builder ret = new MentionPattern.Builder();

      for (final SExpression kid : skip(sExpression.children(), 1)) {
        if (kid.atom().isPresent()) {
          switch (kid.atom().get().asString()) {
            case "SPECIFIC":
              ret.mustBeSpecific(true);
              break;
            case "GENERIC":
              ret.mustBeGeneric(true);
              break;
            case "APPOSITIVE":
              ret.mustBeAppositive(true);
              break;
            case "APPOSITIVE_CHILD":
              ret.mustBeAppositiveChild(true);
              break;
            case "NAMED_APPOSITIVE_CHILD":
              ret.mustBeNamedAppositiveChild(true);
              break;
            case "BLOCK_FALL_THROUGH":
              ret.blockFallThrough(true);
              break;
            default:
              throw new ParseException("Unknown MentionPattern property " + kid.atom().get());
          }
        } else {
          parseAssert(kid.children().size() > 1
              && kid.children().get(0).atom().isPresent(), "Expected sub-expression of"
              + " Mention pattern to have at least two elements, the first atomic", kid);
          //noinspection OptionalGetWithoutIsPresent
          final String subExpressionType = kid.children().get(0).atom().get().asString();
          switch (subExpressionType) {
            case "acetype":
              final ImmutableSet.Builder<EntityType> entityTypes = ImmutableSet.builder();
              for (final SExpression grandkid : skip(kid.children(), 1)) {
                parseAssert(grandkid.atom().isPresent(), "acetype cannot contain non-atoms", kid);
                entityTypes.add(EntityType.of(grandkid.atom().get()));
              }
              ret.entityTypes(entityTypes.build());
              break;
            case "acesubtype":
              final ImmutableSet.Builder<EntitySubtype> entitySubtypes =  ImmutableSet.builder();

              for (final Symbol x : gatherAtomSymbols(kid)) {
                throw new UnsupportedOperationException("Implement me");
              }
              ret.entitySubtypes(entitySubtypes.build());
              break;
            case "mentiontype":
              final ImmutableSet.Builder<Mention.Type> mentionTypes = ImmutableSet.builder();
              for (final Symbol x : gatherAtomSymbols(kid)) {
                mentionTypes.add(Mention.Type.valueOf(x.asString()));
              }
              ret.mentionTypes(mentionTypes.build());
              break;
            case "min-entitylevel":
              parseAssert(kid.children().size() == 2 && kid.children().get(1).atom().isPresent(),
                  "Can only specify one min-entitylevel and it must be atomic", kid);
              final String minEntityLevel = kid.children().get(1).atom().get().asString();
              switch (minEntityLevel) {
                case "NAME":
                  ret.requiresName(true);
                  break;
                case "DESC":
                  ret.requiresNameOrDesc(true);
                  break;
                default:
                  throw new ParseException("Unknown minimum entity level " + minEntityLevel);
              }
              break;
            case "entitylabel":
              ret.entityLabels(gatherAtomSymbols(kid));
              break;
            case "block":
              ret.blockingEntityLabels(gatherAtomSymbols(kid));
              break;
            case "headword":
              for (final Symbol x : gatherAtomSymbols(kid)) {
                final Optional<WordSet> wordSet = lookupWordset(x);
                if (wordSet.isPresent()) {
                  for (final Symbol word : wordSet.get()) {
                    ret.addHeadwordsAccountingForWildcards(word);
                  }
                } else {
                  ret.addHeadwordsAccountingForWildcards(x);
                }
              }
              break;
            case "block_acetype":
              final ImmutableSet.Builder<EntityType> blockedEntityTypes = ImmutableSet.builder();
              for (final Symbol x : gatherAtomSymbols(kid)) {
                blockedEntityTypes.add(EntityType.of(x));
              }
              ret.blockedEntityTypes(blockedEntityTypes.build());
              break;
            case "block_headword":
              for (final Symbol x : gatherAtomSymbols(kid)) {
                final Optional<WordSet> wordSet = lookupWordset(x);
                if (wordSet.isPresent()) {
                  for (final Symbol word : wordSet.get()) {
                    ret.addBlockingHeadwordsAccountingForWildcards(word);
                  }
                } else {
                  ret.addBlockingHeadwordsAccountingForWildcards(x);
                }
              }
              break;
            case "regex":
              parseAssert(kid.children().size() == 2,
                  "Can only have one regex sub-pattern in a mention pattern", kid);
              ret.regexConstraint(new MentionPattern.RegexConstraint.Builder()
                .headRegex(false).regexPattern(parseRegex(kid.children().get(1))).build());
              break;
            case "head-regex":
              parseAssert(kid.children().size() == 2,
                  "Can only have one regex sub-pattern in a mention pattern", kid);
              ret.regexConstraint(new MentionPattern.RegexConstraint.Builder()
                  .headRegex(true).regexPattern(parseRegex(kid.children().get(1))).build());
              break;
            default:
              throw new ParseException("Unknown MentionPattern property " + subExpressionType);
          }
        }
      }
      return ret.build();
    }

    private ImmutableSet<Symbol> gatherAtomSymbols(final SExpression s) {
      checkArgument(s.children().size() > 1 && s.children().get(0).atom().isPresent());
      final ImmutableSet.Builder<Symbol> ret = ImmutableSet.builder();
      for (final SExpression kid : skip(s.children(), 1)) {
        //noinspection OptionalGetWithoutIsPresent
        parseAssert(kid.atom().isPresent(), s.children().get(0).atom().get()
            + " cannot contain non-atoms", s);
        ret.add(kid.atom().get());
      }
      return ret.build();
    }


    private void parseAssert(boolean condition, String msg, SExpression sexp)
        throws ParseException {
      if (!condition) {
        throw new ParseException(msg + " while parsing " + sexp);
      }
    }

  }

  public static class ParseException extends RuntimeException {
    public ParseException(String msg) {
      super(msg);
    }
  }
}

interface WordSet extends Iterable<Symbol> {
  String asRegularExpression();
}
