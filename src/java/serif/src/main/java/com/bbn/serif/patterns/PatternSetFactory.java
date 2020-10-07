package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.sexp.Sexp;
import com.bbn.bue.sexp.SexpAtom;
import com.bbn.bue.sexp.SexpList;
import com.bbn.bue.sexp.SexpUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PatternSetFactory {

  public final static Symbol WORDSETS_SYM = Symbol.from("wordsets");
  public final static Symbol ENTITYLABELS_SYM = Symbol.from("entitylabels");
  public final static Symbol REFERENCE_SYM = Symbol.from("reference");
  public final static Symbol TOPLEVEL_SYM = Symbol.from("toplevel");
  public final static Symbol DOCLEVEL_SYM = Symbol.from("doclevel");
  public final static Symbol BACKOFF_SYM = Symbol.from("backoff");
  public final static Symbol OPTIONS_SYM = Symbol.from("options");

  public static PatternSet fromPattern(Pattern pattern) {
    PatternSet.Builder builder = new PatternSet.Builder(Symbol.from("pattern"));
    builder.withTopLevelPatternAdd(pattern);
    return builder.build();
  }

  public static PatternSet fromSexp(Sexp sexp) {
    SexpUtils.checkList(sexp);
    SexpList lst = (SexpList) sexp;
    if (lst.isEmpty() || !(lst.get(0) instanceof SexpAtom)) {
      throw new PatternSexpParsingException("PatternSet should begin with a pattern set name",
          sexp);
    }
    PatternSet.Builder builder = new PatternSet.Builder(((SexpAtom) lst.get(0)).getValue());

    Map<Symbol, Sexp> patternSetMap = SexpUtils.getSexpArgMap(SexpUtils.getSexpArgs(sexp));
    Map<Symbol, Pattern> referencePatterns = new HashMap<Symbol, Pattern>();
    Map<Symbol, Pattern> entityLabels = new HashMap<Symbol, Pattern>();
    Map<Symbol, Set<Symbol>> wordSets = new HashMap<Symbol, Set<Symbol>>();

    //first we get reference patterns, entity labels, and wordsets
    if (patternSetMap.containsKey(WORDSETS_SYM)) {
      for (Sexp wordSetSexp : SexpUtils.forceList(patternSetMap.get(WORDSETS_SYM))) {
        wordSets.put(SexpUtils.getSexpType(wordSetSexp),
            SexpUtils.getCheckSymSet(SexpUtils.getSexpArgs(wordSetSexp), wordSets));
      }
      builder.withWordSets(wordSets);
    }
    if (patternSetMap.containsKey(ENTITYLABELS_SYM)) {
      for (Sexp elabelSexp : SexpUtils.forceList(patternSetMap.get(ENTITYLABELS_SYM))) {
        Sexp patternSexp = SexpUtils.forceList(SexpUtils.getSexpArgs(elabelSexp)).get(0);
        Pattern pattern = new PatternFactory(referencePatterns, wordSets).fromSexp(patternSexp);
        entityLabels.put(SexpUtils.getSexpType(elabelSexp), pattern);
      }
      builder.withEntityLabels(entityLabels);
    }
    if (patternSetMap.containsKey(REFERENCE_SYM)) {
      for (Sexp patternSexp : SexpUtils.forceList(patternSetMap.get(REFERENCE_SYM))) {
        Pattern pattern = new PatternFactory(referencePatterns, wordSets).fromSexp(patternSexp);
        referencePatterns.put(pattern.getShortcut(), pattern);
      }
      builder.withReferencePatterns(referencePatterns);
    }

    //now we handle the rest of the stuff
    for (Symbol type : patternSetMap.keySet()) {
      if (type.equals(WORDSETS_SYM) || type.equals(ENTITYLABELS_SYM) ||
          type.equals(REFERENCE_SYM)) {
        //already handled
      } else if (type.equals(DOCLEVEL_SYM)) {
        for (Sexp patternSexp : SexpUtils.forceList(patternSetMap.get(type))) {
          Pattern pattern = new PatternFactory(referencePatterns, wordSets).fromSexp(patternSexp);
          builder.withDocPatternAdd(pattern);
        }
      } else if (type.equals(TOPLEVEL_SYM)) {
        for (Sexp patternSexp : SexpUtils.forceList(patternSetMap.get(type))) {
          Pattern pattern = new PatternFactory(referencePatterns, wordSets).fromSexp(patternSexp);
          builder.withTopLevelPatternAdd(pattern);
        }
      } else if (type.equals(BACKOFF_SYM)) {
        System.err
            .println("WARNING: Pattern Set - backoff currently not supported and will be ignored.");
      } else if (type.equals(OPTIONS_SYM)) {
        System.err
            .println("WARNING: Pattern Set - options currently not supported and will be ignored.");
      } else {
        throw new PatternSexpParsingException("Unexpected child of pattern set: " + type.toString(),
            sexp);
      }
    }

    return builder.build();
  }
}
