package com.bbn.serif.constraints;

import com.bbn.bue.common.Finishable;
import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.TextSerifIngester;
import com.bbn.serif.entities.EntityFinder;
import com.bbn.serif.events.EventFinder;
import com.bbn.serif.events.EventMentionFinder;
import com.bbn.serif.languages.SerifLanguage;
import com.bbn.serif.mentions.MentionFinder;
import com.bbn.serif.names.NameFinder;
import com.bbn.serif.parse.Parser;
import com.bbn.serif.regions.RegionFinder;
import com.bbn.serif.relations.RelationFinder;
import com.bbn.serif.relations.RelationMentionFinder;
import com.bbn.serif.sentences.SentenceFinder;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.tokens.TokenFinder;
import com.bbn.serif.values.ValueMentionFinder;

import com.google.common.annotations.Beta;
import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;
import javax.inject.Qualifier;

import static com.bbn.bue.common.strings.offsets.CharOffset.asCharOffset;
import static com.google.common.base.Preconditions.checkNotNull;


/**
 * The basic document converter for Constrained Serif, which runs the rough equivalent of the following Serif
 * stages:
 * <pre>
 *   * RegionFinding - see {@code RegionFinder}
 *   * Sentence breaking - see {@code SentenceSegmenter}
 *   * Tokenization - see {@code Tokenizer}
 *   * Name fidning - see {@code ConstrainedNameFinder}
 *   * Value finding - see {@code ValueMentionFinder}
 *   * Parsing - see {@code ConstrainedParser}
 *   * Mentions - see {@code MentionFinder}
 *   * Events - see {@code EventFinder}
 *   * Entities - see {@code EntityFinder}
 *   * Relations - see {@code RelationFinder}
 * </pre>
 *
 * Notably skipped portions, due to scope:
 * <pre>
 *   * Part of Speech tagging (simply not relevant)
 *   * Nested-Names (not tagged in our data)
 *   * Any special parsing
 *   * Actor matching
 * </pre>
 *
 * Typically for the "XFinder"s you will use constrained implementations.
 *
 * This code is very experimental and subject to change. Do not use without consulting rgabbard
 * or jdeyoung.
 */
@Beta
public final class ConstrainedSerif implements Finishable {

  private static final Logger log = LoggerFactory.getLogger(ConstrainedSerif.class);

  private final SerifLanguage serifLanguage;
  private final int charsToSkip;
  private final TextSerifIngester textIngester;
  private final RegionFinder regionFinder;
  private final SentenceFinder sentenceSegmenter;
  private final TokenFinder tokenizer;
  private final NameFinder constrainedNameFinder;
  private final ValueMentionFinder valueMentionFinder;
  private final Parser constrainedParser;
  private final MentionFinder mentionFinder;
  private final EntityFinder entityFinder;
  private final EventMentionFinder eventMentionFinder;
  private final EventFinder eventFinder;
  private final RelationMentionFinder relationMentionFinder;
  private final RelationFinder relationFinder;


  @Inject
  private ConstrainedSerif(
      @CharsToSkipP final int charsToSkip,
      final SerifLanguage serifLanguage,
      TextSerifIngester textIngester,
      final RegionFinder regionFinder,
      final SentenceFinder sentenceSegmenter,
      final TokenFinder tokenizer,
      final NameFinder constrainedNameFinder,
      final ValueMentionFinder valueMentionFinder,
      final Parser constrainedParser,
      final MentionFinder mentionFinder,
      final EntityFinder entityFinder,
      final EventMentionFinder eventMentionFinder,
      final EventFinder eventFinder,
      final RelationMentionFinder relationMentionFinder,
      final RelationFinder relationFinder) {
    this.serifLanguage = checkNotNull(serifLanguage);
    this.charsToSkip = charsToSkip;
    this.textIngester = checkNotNull(textIngester);
    this.regionFinder = regionFinder;
    this.sentenceSegmenter = sentenceSegmenter;
    this.tokenizer = tokenizer;
    this.constrainedNameFinder = constrainedNameFinder;
    this.valueMentionFinder = valueMentionFinder;
    this.constrainedParser = constrainedParser;
    this.mentionFinder = mentionFinder;
    this.entityFinder = entityFinder;
    this.eventMentionFinder = eventMentionFinder;
    this.eventFinder = eventFinder;
    this.relationMentionFinder = relationMentionFinder;
    this.relationFinder = relationFinder;
  }

  public DocTheory processFile(final File input, final Symbol docID,
      final SerifConstraints serifConstraints) throws IOException {
    // removes parens so the parses don't break.
    // TODO put this in the tokenization stage?
    final UnicodeFriendlyString contents =
        StringUtils.unicodeFriendly(Files.toString(input, Charsets.UTF_8).replaceAll("\\(", " ").replaceAll("\\)", " "));
    final DocTheory withText =
        textIngester.ingestToDocTheory(docID, contents.substringByCodePoints(asCharOffset(charsToSkip)));
    final DocTheory withRegions = regionFinder.addRegions(withText,
        serifConstraints.regionConstraints());
    final DocTheory withSentences = sentenceSegmenter.segmentSentences(withRegions,
        serifConstraints.segmentationConstraints());
    final DocTheory tokenized = tokenizer.tokenize(withSentences,
        serifConstraints.tokenizationConstraints());
    final DocTheory withNames =
        constrainedNameFinder.addNames(tokenized, serifConstraints.nameConstraints());
    final DocTheory withValues =
        valueMentionFinder.addValues(withNames, serifConstraints.valueConstraints());
    final DocTheory withParse =
        constrainedParser.addParse(withValues, serifConstraints.parseConstraints());
    final DocTheory withMentions =
        mentionFinder.addMentions(withParse, serifConstraints.mentionConstraints());
    final DocTheory sealed = withMentions;
    final DocTheory withEntities =
        entityFinder.addEntities(sealed, serifConstraints.entityConstraints());

    final DocTheory withEventMentions =
        eventMentionFinder
            .addEventMentions(withEntities, serifConstraints.eventMentionConstraints());
    final DocTheory withEvents =
        eventFinder.addEvents(withEventMentions, serifConstraints.eventConstraints());

    final DocTheory withRelationMentions = relationMentionFinder
        .addRelationMentions(withEvents, serifConstraints.relationMentionConstraints());
    final DocTheory withRelations =
        relationFinder.addRelations(withRelationMentions, serifConstraints.relationConstraints());

    return withRelations;
  }

  @Override
  public void finish() throws IOException {
    textIngester.finish();
    regionFinder.finish();
    sentenceSegmenter.finish();
    tokenizer.finish();
    constrainedNameFinder.finish();
    valueMentionFinder.finish();
    constrainedParser.finish();
    mentionFinder.finish();
    entityFinder.finish();
    eventMentionFinder.finish();
    eventFinder.finish();
    relationMentionFinder.finish();
    relationFinder.finish();
  }

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CharsToSkipP {

    String param = "com.bbn.serif.jacserif.originalTextAdjust";
  }

  @Qualifier
  @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface GlobalTolerance {

    String param = "com.bbn.serif.jacserif.tolerant";
  }


}
