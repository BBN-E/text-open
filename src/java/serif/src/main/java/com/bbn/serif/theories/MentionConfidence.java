package com.bbn.serif.theories;

import com.bbn.nlp.languages.LanguageSpecific;

@LanguageSpecific("English")
public enum MentionConfidence {
  /**
   * any name mention *
   */
  AnyName,
  /**
   * <i>President</i> Barack Obama *
   */
  TitleDesc,
  /**
   * e.g. Microsoft is <i>a big software company</i> *
   */
  CopulaDesc,
  /**
   * e.g. Microsoft, <i>a big software company</i> *
   */
  ApposDesc,
  /**
   * only one candidate of this type in document before desc (possibly excluding dateline ORGs)*
   */
  OnlyOneCandidateDesc,
  /**
   * adjacent sentences such as "Obama anounced .." "<i>The President</i> was visiting..." *
   */
  PrevSentDoubleSubjectDesc,
  /**
   * any other descriptor*
   */
  OtherDesc,
  /**
   * Microsoft, _which_ is a big software company*
   */
  WhqLinkPron,
  /**
   * Bob and <i>his</i> dog *
   */
  NameAndPossPron,
  /**
   * Bob said that <i></i>he</i> would go shopping (two subjects, both persons, no other name
   * preceding the pronoun)*
   */
  DoubleSubjectPersonPron,
  /**
   * only one candidate of this type in document before pronoun (possibly excluding dateline ORGs)
   * *
   */
  OnlyOneCandidatePron,
  /**
   * adjacent sentences such as "Obama said..." " <i>He</i> denied .." *
   */
  PrevSentDoubleSubjectPron,
  /**
   * any other pronoun *
   */
  OtherPron,
  /**
   * not coreferent with another entity *
   */
  NoEntity,
  /**
   * An ambiguous name
   */
  AmbiguousName,
  /**
   * anything not in another category. Also, the default if not specified in the input *
   */
  UnknownConfidence;


  public static final MentionConfidence DEFAULT = UnknownConfidence;

  private static final MentionConfidence[] values = MentionConfidence.values();

  /**
   * Provide support for legacy format which used integers for MentionConfidence. Don't use these
   * for any other purpose.
   */
  public static MentionConfidence parseOrdinal(int i) {
    return MentionConfidence.values[i];
  }
};

