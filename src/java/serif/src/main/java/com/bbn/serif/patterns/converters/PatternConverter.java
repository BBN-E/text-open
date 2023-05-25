package com.bbn.serif.patterns.converters;

import com.bbn.serif.patterns.ArgumentPattern;
import com.bbn.serif.patterns.CombinationPattern;
import com.bbn.serif.patterns.EventPattern;
import com.bbn.serif.patterns.EventEventRelationPattern;
import com.bbn.serif.patterns.IntersectionPattern;
import com.bbn.serif.patterns.MentionPattern;
import com.bbn.serif.patterns.Pattern;
import com.bbn.serif.patterns.PropPattern;
import com.bbn.serif.patterns.RegexPattern;
import com.bbn.serif.patterns.TextPattern;
import com.bbn.serif.patterns.UnionPattern;
import com.bbn.serif.patterns.ValueMentionPattern;

public abstract class PatternConverter<T> {

  public abstract T convertArgumentPattern(ArgumentPattern pattern);

  public abstract T convertCombinationPattern(CombinationPattern pattern);

  public abstract T convertEventPattern(EventPattern pattern);

  public abstract T convertEventEventRelationPattern(EventEventRelationPattern pattern);

  public abstract T convertIntersectionPattern(IntersectionPattern pattern);

  public abstract T convertMentionPattern(MentionPattern pattern);

  public abstract T convertPropPattern(PropPattern pattern);

  public abstract T convertRegexPattern(RegexPattern pattern);

  public abstract T convertTextPattern(TextPattern pattern);

  public abstract T convertUnionPattern(UnionPattern pattern);

  public abstract T convertValuePattern(ValueMentionPattern pattern);

  public T convert(Pattern pattern) {
    if (pattern instanceof ArgumentPattern) {
      return convertArgumentPattern((ArgumentPattern) pattern);
    } else if (pattern instanceof CombinationPattern) {
      return convertCombinationPattern((CombinationPattern) pattern);
    } else if (pattern instanceof EventPattern) {
      return convertEventPattern((EventPattern) pattern);
    } else if (pattern instanceof EventEventRelationPattern) {
      return convertEventEventRelationPattern((EventEventRelationPattern) pattern);
    } else if (pattern instanceof IntersectionPattern) {
      return convertIntersectionPattern((IntersectionPattern) pattern);
    } else if (pattern instanceof MentionPattern) {
      return convertMentionPattern((MentionPattern) pattern);
    } else if (pattern instanceof PropPattern) {
      return convertPropPattern((PropPattern) pattern);
    } else if (pattern instanceof RegexPattern) {
      return convertRegexPattern((RegexPattern) pattern);
    } else if (pattern instanceof TextPattern) {
      return convertTextPattern((TextPattern) pattern);
    } else if (pattern instanceof UnionPattern) {
      return convertUnionPattern((UnionPattern) pattern);
    } else if (pattern instanceof ValueMentionPattern) {
      return convertValuePattern((ValueMentionPattern) pattern);
    } else {
      throw new RuntimeException("Unhandled pattern type: " + pattern);
    }
  }
}
