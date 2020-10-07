package com.bbn.serif.patterns;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.patterns.converters.SexpPatternConverter;

import com.google.common.collect.ImmutableMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PatternSet implements Iterable<Pattern> {

  private final Symbol patternSetName;
  private final Collection<Pattern> topLevelPatterns;
  private final Collection<Pattern> docPatterns;
  private final Map<Symbol, Pattern> referencePatterns;
  private final Map<Symbol, Pattern> entityLabels;
  private final Map<Symbol, Set<Symbol>> wordSets;

  //Entity labels not yet supported in Java side
  //private Collection<Pattern> entityLabelPatterns;
  //private Set<Symbol> entityLabels;

  @Override
  public Iterator<Pattern> iterator() {
    return topLevelPatterns.iterator();
  }

  public Symbol getPatternSetName() {
    return patternSetName;
  }

  public Collection<Pattern> getDocPatterns() {
    return docPatterns;
  }

  public Map<Symbol, Pattern> getReferencePatterns() {
    return referencePatterns;
  }

  public Map<Symbol, Pattern> getEntityLabels() {
    return entityLabels;
  }

  public Map<Symbol, Set<Symbol>> getWordSets() {
    return wordSets;
  }

  public Collection<Pattern> getTopLevelPatterns() {
    return topLevelPatterns;
  }

  public Pattern getFirstPattern() {
    return topLevelPatterns.iterator().next();
  }

  public Builder modifiedCopyBuilder() {
    Builder b = new Builder(patternSetName);
    for (Pattern p : getTopLevelPatterns()) {
      b.withTopLevelPatternAdd(p);
    }
    for (Pattern p : getDocPatterns()) {
      b.withDocPatternAdd(p);
    }
    for (Symbol ref : referencePatterns.keySet()) {
      b.withReferencePatternAdd(ref, referencePatterns.get(ref));
    }
    for (Symbol elabel : entityLabels.keySet()) {
      b.withEntityLabelAdd(elabel, entityLabels.get(elabel));
    }
    for (Symbol wset : wordSets.keySet()) {
      b.withWordSetAdd(wset, wordSets.get(wset));
    }
    return b;
  }

  public PatternSet(Symbol patternSetName, Collection<Pattern> topLevelPatterns,
      Collection<Pattern> docPatterns, Map<Symbol, Pattern> referencePatterns,
      Map<Symbol, Pattern> entityLabels, Map<Symbol, Set<Symbol>> wordSets) {
    this.patternSetName = patternSetName;
    this.topLevelPatterns = topLevelPatterns;
    this.docPatterns = docPatterns;
    this.referencePatterns = ImmutableMap.<Symbol, Pattern>copyOf(referencePatterns);
    ;
    this.entityLabels = ImmutableMap.<Symbol, Pattern>copyOf(entityLabels);
    this.wordSets = ImmutableMap.<Symbol, Set<Symbol>>copyOf(wordSets);
    ;
  }

  public PatternSet(Symbol patternSetName, Collection<Pattern> patterns) {
    this(patternSetName, patterns, new HashSet<Pattern>(),
        new HashMap<Symbol, Pattern>(), new HashMap<Symbol, Pattern>(),
        new HashMap<Symbol, Set<Symbol>>());
  }

  //temporary toString for convenience until we write more complete ones
  @Override
  public String toString() {
    return new SexpPatternConverter().convert(this).toString();
  }


  public static class Builder {

    private final Symbol patternSetName;
    private final Collection<Pattern> topLevelPatterns;
    private final Collection<Pattern> docPatterns;
    private Map<Symbol, Pattern> referencePatterns;
    private Map<Symbol, Pattern> entityLabels;
    private Map<Symbol, Set<Symbol>> wordSets;

    public Builder(Symbol name) {
      this.patternSetName = name;
      this.topLevelPatterns = new HashSet<Pattern>();
      this.docPatterns = new HashSet<Pattern>();
      this.referencePatterns = new HashMap<Symbol, Pattern>();
      this.entityLabels = new HashMap<Symbol, Pattern>();
      this.wordSets = new HashMap<Symbol, Set<Symbol>>();
    }

    public PatternSet build() {
      return new PatternSet(patternSetName, topLevelPatterns, docPatterns,
          referencePatterns, entityLabels, wordSets);
    }

    public Builder withTopLevelPatternsClear() {
      topLevelPatterns.clear();
      return this;
    }

    public Builder withTopLevelPatternAdd(Pattern pattern) {
      topLevelPatterns.add(pattern);
      return this;
    }

    public Builder withDocPatternsClear() {
      docPatterns.clear();
      return this;
    }

    public Builder withDocPatternAdd(Pattern pattern) {
      docPatterns.add(pattern);
      return this;
    }

    public Builder withReferencePatterns(Map<Symbol, Pattern> referencePatterns) {
      this.referencePatterns = referencePatterns;
      return this;
    }

    public Builder withReferencePatternAdd(Symbol name, Pattern pattern) {
      referencePatterns.put(name, pattern);
      return this;
    }

    public Builder withEntityLabels(Map<Symbol, Pattern> entityLabels) {
      this.entityLabels = entityLabels;
      return this;
    }


    public Builder withEntityLabelAdd(Symbol name, Pattern pattern) {
      entityLabels.put(name, pattern);
      return this;
    }

    public Builder withWordSets(Map<Symbol, Set<Symbol>> wordSets) {
      this.wordSets = wordSets;
      return this;
    }

    private void wordSetAdd(Symbol name, Symbol word) {
      if (!wordSets.containsKey(name)) {
        wordSets.put(name, new HashSet<Symbol>());
      }
      wordSets.get(name).add(word);
    }

    public Builder withWordSetAdd(Symbol name, Set<Symbol> words) {
      for (Symbol w : words) {
        wordSetAdd(name, w);
      }
      return this;
    }

    public Builder withWordSetAdd(Symbol name, Symbol word) {
      wordSetAdd(name, word);
      return this;
    }

  }

}
