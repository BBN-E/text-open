package com.bbn.serif.io;


import com.bbn.bue.common.OptionalUtils;
import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.UnicodeFriendlyString;
import com.bbn.bue.common.exceptions.NotImplementedException;
import com.bbn.bue.common.parameters.Parameters;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.ByteOffset;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.EDTOffset;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.strings.offsets.OffsetGroupSpan;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.temporal.Timex2Time;
import com.bbn.bue.common.xml.XMLUtils;
import com.bbn.serif.common.Segment;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.languages.SerifLanguage;
import com.bbn.serif.theories.Dependencies;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.Document;
import com.bbn.serif.theories.DocumentActorInfo;
import com.bbn.serif.theories.DocumentEvent;
import com.bbn.serif.theories.DocumentEventArguments;
import com.bbn.serif.theories.DocumentEvents;
import com.bbn.serif.theories.Entities;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Event;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.EventMentions;
import com.bbn.serif.theories.Events;
import com.bbn.serif.theories.Gloss;
import com.bbn.serif.theories.LexicalForm;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Mention.MetonymyInfo;
import com.bbn.serif.theories.MentionConfidence;
import com.bbn.serif.theories.Mentions;
import com.bbn.serif.theories.Metadata;
import com.bbn.serif.theories.Morph;
import com.bbn.serif.theories.MorphFeature;
import com.bbn.serif.theories.MorphToken;
import com.bbn.serif.theories.MorphTokenAnalysis;
import com.bbn.serif.theories.MorphTokenSequence;
import com.bbn.serif.theories.MorphType;
import com.bbn.serif.theories.MorphologyAlgorithmDescription;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.Names;
import com.bbn.serif.theories.NestedName;
import com.bbn.serif.theories.NestedNames;
import com.bbn.serif.theories.Parse;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.Propositions;
import com.bbn.serif.theories.Region;
import com.bbn.serif.theories.Relation;
import com.bbn.serif.theories.RelationMention;
import com.bbn.serif.theories.RelationMentions;
import com.bbn.serif.theories.Relations;
import com.bbn.serif.theories.Sentence;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SentenceTheoryBeam;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.TokenSpan;
import com.bbn.serif.theories.TokenSpans;
import com.bbn.serif.theories.Value;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.ValueMentions;
import com.bbn.serif.theories.Values;
import com.bbn.serif.theories.Zone;
import com.bbn.serif.theories.Zoning;
import com.bbn.serif.theories.acronyms.Acronym;
import com.bbn.serif.theories.actors.ActorEntities;
import com.bbn.serif.theories.actors.ActorEntity;
import com.bbn.serif.theories.actors.ActorMention;
import com.bbn.serif.theories.actors.ActorMentions;
import com.bbn.serif.theories.actors.CompositeActorMention;
import com.bbn.serif.theories.actors.GeoResolvedActor;
import com.bbn.serif.theories.actors.ProperNounActorMention;
import com.bbn.serif.theories.actors.SimpleActorMention;
import com.bbn.serif.theories.facts.Fact;
import com.bbn.serif.theories.facts.Facts;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMention;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMentionArgument;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMentions;
import com.bbn.serif.theories.icewseventmentions.ICEWSEventMention;
import com.bbn.serif.theories.icewseventmentions.ICEWSEventMentions;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.EventEventRelationMentions;
import com.bbn.serif.types.*;

import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import org.joda.time.DateTime;
import org.joda.time.IllegalInstantException;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static com.bbn.bue.common.xml.XMLUtils.checkMissing;
import static com.bbn.bue.common.xml.XMLUtils.childrenWithTag;
import static com.bbn.bue.common.xml.XMLUtils.directChild;
import static com.bbn.bue.common.xml.XMLUtils.dumpElement;
import static com.bbn.bue.common.xml.XMLUtils.dumpXMLElement;
import static com.bbn.bue.common.xml.XMLUtils.elementChildren;
import static com.bbn.bue.common.xml.XMLUtils.hasAnyChildElement;
import static com.bbn.bue.common.xml.XMLUtils.is;
import static com.bbn.bue.common.xml.XMLUtils.nonEmptySymbolOrNull;
import static com.bbn.bue.common.xml.XMLUtils.optionalBooleanAttribute;
import static com.bbn.bue.common.xml.XMLUtils.optionalDoubleAttribute;
import static com.bbn.bue.common.xml.XMLUtils.optionalIntegerAttribute;
import static com.bbn.bue.common.xml.XMLUtils.optionalLongAttribute;
import static com.bbn.bue.common.xml.XMLUtils.optionalStringAttribute;
import static com.bbn.bue.common.xml.XMLUtils.optionalSymbolAttribute;
import static com.bbn.bue.common.xml.XMLUtils.optionalSymbolList;
import static com.bbn.bue.common.xml.XMLUtils.requiredAttribute;
import static com.bbn.bue.common.xml.XMLUtils.requiredBooleanAttribute;
import static com.bbn.bue.common.xml.XMLUtils.requiredDoubleAttribute;
import static com.bbn.bue.common.xml.XMLUtils.requiredFloatAttribute;
import static com.bbn.bue.common.xml.XMLUtils.requiredIntegerAttribute;
import static com.bbn.bue.common.xml.XMLUtils.requiredLongAttribute;
import static com.bbn.bue.common.xml.XMLUtils.requiredStringList;
import static com.bbn.bue.common.xml.XMLUtils.requiredSymbolAttribute;
import static com.bbn.serif.io.SerifXML.DOC_EVENT_ARG_ELEMENT;
import static com.bbn.serif.io.SerifXML.DOC_EVENT_ARG_SET_ELEMENT;
import static com.bbn.serif.io.SerifXML.DOC_EVENT_ELEMENT;
import static com.bbn.serif.io.SerifXML.DOC_EVENT_SET_ELEMENT;
import static com.bbn.serif.io.SerifXML.OFFSET_INTO_SOURCE;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Turns SerifXML documents into {@link DocTheory}s.
 *
 * You rarely want to create a {@link SerifXMLLoader} directly; instead prefer to inject one
 * using e.g. {@link SerifXMLIOFromParamsM}, which is itself installed automatically by the
 * standard {@link com.bbn.serif.SerifEnvironmentM}.
 *
 * If you need to create a {@code SerifXMLLoader} without injection (e.g. for tests), use
 * {@link #builder()}.
 *
 * Concerning 'sloppy offsets': This is off by default. If turned on, cases where a {@link
 * LocatedString} we load up does not have offsets matching those given in the SerifXML are warnings
 * rather than errors. This is sometimes necessary when processing documents from old (pre-2015)
 * versions of Serif. When LocatedString are stored in SerifXML, two different strategies are used
 * when reloading them. If they are exact substrings of the original text, we just treat them as
 * such substrings. If they are not, we expect them to include their offset information in the
 * format of {@code OffsetEntry} elements.  Older versions of Serif did not include OffsetEntry
 * elements in the second case and the loader (in CSerif and JSerif) instead tries to reconstruct
 * what the offsets should be from scratch. Sometimes, especially for regions, what is written to
 * the SerifXML by CSerif results in incorrect offsets.
 *
 * Concerning 'compatibility with documents offset into source': This is off by default. This exists
 * to load documents with source text that is at some offset > 0 into an original source. The loader
 * translates this source from its input offsets into zero-based offsets on all counts (char, edt,
 * byte), and stores this on the {@link Document} object. All offsets are then transformed as if the
 * offending document began at zero-index. There's no current way to translate them back; however
 * the information needed to do this is stored on the {@link Document} itself. Implement it if you
 * need it. These documents are technically illegal serifxml, but were produced by old processes
 * that existed before the semantics on {@link LocatedString} were well defined.
 */
// the generics stuff used by fetch() is not ideal, but it saves a lot of typing for the user
// and it works...
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "TypeParameterUnusedInFormals"})
@TextGroupImmutable
@org.immutables.value.Value.Immutable
public abstract class SerifXMLLoader implements DocTheoryLoader {

  @org.immutables.value.Value.Default
  public boolean allowSloppyOffsets() {
    return false;
  }

  /**
   * A compatibility mode that exists to read SerifXML over text that is offset into a bigger
   * document (this is technically illegal). This is for backwards compatibility.  See class
   * Javadoc for details.
   */
  @org.immutables.value.Value.Default
  public boolean compatibilityWithDocumentsOffsetIntoSource() {
    return false;
  }

  /**
   * SerifXML files contain a string within them specifying the language of the document; this
   * specifies how to map these strings to {@link SerifLanguage} objects.  If we cannot perform
   * this mapping, loading the document will fail.  If you inject your {@code SerifXMLLoader} in
   * the usual way, this will automatically be populated with the mappings for all registered
   * languages.
   */
  @SuppressWarnings("deprecation")
  @org.immutables.value.Value.Default
  public SerifLanguage.SerifLanguageMap languageLookupMap() {
    return SerifLanguage.byLongNames();
  }


  private static final Logger log = LoggerFactory.getLogger(SerifXMLLoader.class);

  public static final String ALLOW_SLOPPY_OFFSETS_PARAM = "com.bbn.serif.io.allowSloppyOffsets";


  public static Builder builder() {
    return new Builder();
  }

  /**
   * @deprecated Prefer {@link #loadFrom(CharSource)}
   */
  @Deprecated
  public DocTheory loadFromResource(final String resourceString) throws IOException {
    return loadFrom(Resources.asCharSource(Resources.getResource(resourceString), Charsets.UTF_8));
  }

  public DocTheory loadFrom(final File f) throws IOException {
    try {
      return loadFrom(Files.asCharSource(f, Charsets.UTF_8));
    } catch (final Exception e) {
      throw new IOException(
          String.format("Error loading SerifXML document %s", f.getAbsolutePath()), e);
    }
  }

  // loadFrom(String) is deprecated for public use but still fine for internal use
  @SuppressWarnings("deprecation")
  @Override
  public DocTheory loadFrom(final CharSource source) throws IOException {
    try {
      return loadFrom(source.read());
    } catch (final Exception e) {
      throw new IOException(
          String.format("Error loading SerifXML document %s", source), e);
    }
  }

  /**
   * Prefer the clearer {@link #loadFromString(String)} to avoid errors where the user means to call
   * {@link #loadFrom(java.io.File)} but passes a string instead.
   */
  @Deprecated
  public DocTheory loadFrom(final String s) throws IOException {
    return loadFromString(s);
  }

  public DocTheory loadFromString(final String s) throws IOException {
    // The XML parser treats \r\n as a single character. This is problematic
    // when we are using character offsets. To avoid this, we replace
    // \r with an entity reference before parsing
    final int contentStart = s.indexOf("<SerifXML");
    if (contentStart < 0) {
      throw new IOException("Cannot parse string without <SerifXML> element");
    }
    final String newContent;
    if (s.substring(0, contentStart).indexOf("\r\n") > 0) {
                        /* This file was written with Windows line endings, probably by this
                         * library.  Thus it hopefully already has entity references for the \r\ns, we should not add more.
			 */
      newContent = s;
    } else {
                        /* This file was written with unix line endings.
			 * Even if it has already been processed by jserif, there shouldn't
			 * be any issue by replacing any remaining windows line endings.
			 */
      newContent = s.replaceAll("\r", "&#xD;");
    }
    final InputSource in = new InputSource(new StringReader(newContent));
    try {
      final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      final DocumentBuilder builder = factory.newDocumentBuilder();
      return new SerifXMLLoading().loadFrom(builder.parse(in));
    } catch (final ParserConfigurationException | SAXException e) {
      throw new SerifXMLException("Error parsing xml", e);
    }
  }

  /**
   * Throws a SerifException when failing to process a CharSource Handle the iteration yourself if
   * you must deal with... problematic documents
   */
  public Function<CharSource, DocTheory> asFunctionFromCharSource() {
    return new Function<CharSource, DocTheory>() {
      @Override
      public DocTheory apply(final CharSource input) {
        try {
          return loadFrom(input);
        } catch (IOException e) {
          throw new SerifException("Failed to process CharSource", e);
        }
      }
    };
  }

  // constants used by SerifXMLLoading; here because non-static inner classes cannot have static
  // members
  private static final DateTimeFormatter formatter1 =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
  private static final DateTimeFormatter formatter2 =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
  private static final DateTimeFormatter formatter3 =
      DateTimeFormat.forPattern("yyyyMMdd'T'HHmmss");
  private static final DateTimeFormatter formatter4 =
      DateTimeFormat.forPattern("yyyy-MM-dd'T':HH:mm-zz:zz");
  private static final Splitter ON_COLON = Splitter.on(':').omitEmptyStrings().trimResults();
  private static final Splitter SPACE_SPLITTER = Splitter.on(" ").trimResults().omitEmptyStrings();
  private static final Pattern OFFSET_PATTERN = Pattern.compile("\\A(\\d+):(\\d+)\\Z");
  private static final ImmutableSet<String> TAGS_ALLOWED_TO_HAVE_BAD_OFFSETS_WITH_SLOPPY_OFFSETS =
      ImmutableSet.of("Region", "Sentence");

  /**
   * Holds state information for a single instance of loading a SerifXML document. We do this
   * to allow SerifXML loaders to be thread-safe.
   */
  final class SerifXMLLoading {

    private final Map<String, Object> idMap = new HashMap<>();
    private final SortedSet<String> warnings = new TreeSet<>();
    private Optional<OffsetGroup> sourceDocumentOffsetShift = Optional.absent();

    public synchronized DocTheory loadFrom(final org.w3c.dom.Document xml) {
      final Element root = xml.getDocumentElement();
      final String rootTag = root.getTagName();

      final DocTheory ret;
      if (rootTag.equalsIgnoreCase("SerifXML")) {
        final Optional<Element> documentChild = directChild(root, "Document");
        if (documentChild.isPresent()) {
          ret = loadFrom(documentChild.get());
        } else {
          throw new SerifXMLException(
              "If a SerifXML has SerifXML tag at the top-level, it must have a Document element immediately below it");
        }
      } else if (rootTag.equalsIgnoreCase("Document")) {
        ret = loadFrom(root);
      } else {
        throw new SerifXMLException("SerifXML should have a root of SerifXML or Document");
      }
      reportWarnings(ret.docid());
      return ret;
    }

    void reportWarnings(Symbol docId) {
      if (!warnings.isEmpty()) {
        final StringBuilder msg = new StringBuilder();
        msg.append("For document ").append(docId).append(" got the following warnings:\n");
        for (final String warning : warnings) {
          // deterministic because warnings is a sorted set
          msg.append("\t").append(warning).append("\n");
        }
        log.warn(msg.toString());
      }
    }

    void warn(String msg) {
      warnings.add(msg);
    }

    public synchronized DocTheory loadFrom(final Element xml) {
      idMap.clear();
      final Document document = toDocument(xml);
      final DocTheory.Builder builder = DocTheory.builderForDocument(document);

      // because the EventMentionArguments in SentenceTheories may refer to ValueMentions
      // in the document-level value mentions, but we can't createWithMentions these ValueMentions without
      // the sentence-level token sequences, we need to createWithMentions all the token sequences up front
      final List<TokenSequence> tokenSequences = gatherTokenSequences(xml, document.originalText());

      // a lot of morphology structures are shared, so we store a "library" of morphology structures
      // which are referred to by sentence-level morphology objects. We don't return anything here
      // because we are just interested in loading the objects into the ID map
      final Optional<Element> morphologyAlgorithmsEl = directChild(xml, "Algorithms");
      if (morphologyAlgorithmsEl.isPresent()) {
        loadAlgorithms(morphologyAlgorithmsEl.get());
      }
      final Optional<Element> morphEl = directChild(xml, "MorphTokenAnalyses");
      if (morphEl.isPresent()) {
        loadMorphologyInformation(morphEl.get());
      }

      final Optional<Element> valueMentionsElement = directChild(xml, "ValueMentionSet");
      if (valueMentionsElement.isPresent()) {
        builder.valueMentions(
            toDocumentLevelValueMentionSet(valueMentionsElement.get(), tokenSequences));
      } else {
        builder.valueMentions(ValueMentions.absent());
      }

      final List<SentenceTheoryBeam> sentenceTheoryBeams = Lists.newArrayList();
      final Optional<Element> sentencesElement = directChild(xml, "Sentences");
      if (sentencesElement.isPresent()) {
        int sentenceIdx = 0;
        for (Node child = sentencesElement.get().getFirstChild(); child != null;
             child = child.getNextSibling()) {
          if (child instanceof Element) {
            sentenceTheoryBeams.add(toSentenceTheoryBeam((Element) child, sentenceIdx, document));
            ++sentenceIdx;
          }
        }
      }
      builder.sentenceTheoryBeams(sentenceTheoryBeams);

      final Optional<Element> valueSetElement = directChild(xml, "ValueSet");
      if (valueSetElement.isPresent()) {
        builder.values(toValueSet(valueSetElement.get()));
      } else {
        builder.values(Values.absent());
      }

      final Optional<Element> entitySetElement = directChild(xml, "EntitySet");
      if (entitySetElement.isPresent()) {
        builder.entities(toEntitySet(entitySetElement.get()));
      } else {
        builder.entities(Entities.absent());
      }

      final Optional<Element> relationSetElement = directChild(xml, "RelationSet");
      if (relationSetElement.isPresent()) {
        builder.relations(toRelations(relationSetElement.get()));
      } else {
        builder.relations(Relations.absent());
      }

      final Optional<Element> eventSetElement = directChild(xml, "EventSet");
      if (eventSetElement.isPresent()) {
        builder.events(toEvents(eventSetElement.get()));
      } else {
        builder.events(Events.absent());
      }

      final Optional<Element> actorEntitySetElement = directChild(xml, "ActorEntitySet");
      if (actorEntitySetElement.isPresent()) {
        builder.actorEntities(toActorEntities(actorEntitySetElement.get()));
      } else {
        builder.actorEntities(ActorEntities.absent());
      }

      final Optional<Element> factSetElement = directChild(xml, "FactSet");
      if (factSetElement.isPresent()) {
        builder.facts(toFacts(factSetElement.get(), tokenSequences));
      } else {
        builder.facts(Facts.absent());
      }

      final Optional<Element> actorMentionSetElement = directChild(xml, "ActorMentionSet");
      if (actorMentionSetElement.isPresent()) {
        builder.actorMentions(toActorMentions(actorMentionSetElement.get()));
      } else {
        builder.actorMentions(ActorMentions.absent());
      }

      final Optional<Element> icewsEventMentionSetElement =
          directChild(xml, "ICEWSEventMentionSet");
      if (icewsEventMentionSetElement.isPresent()) {
        builder.icewsEventMentions(toICEWSEventMentions(icewsEventMentionSetElement.get()));
      } else {
        builder.icewsEventMentions(ICEWSEventMentions.absent());
      }

      final Optional<Element> eventEventRelationMentionSetElement =
          directChild(xml, "EventEventRelationMentionSet");
      if (eventEventRelationMentionSetElement.isPresent()) {
        builder.eventEventRelationMentions(
            toEventEventRelationMentions(eventEventRelationMentionSetElement.get()));
      } else {
        builder.eventEventRelationMentions(EventEventRelationMentions.absent());
      }

      final Optional<Element> documentActorInfoElement = directChild(xml, "DocumentActorInfo");
      if (documentActorInfoElement.isPresent()) {
        builder.documentActorInfo(toDocumentActorInfo(documentActorInfoElement.get()));
      }

      final Optional<Element> flexibleEventMentionSetElement =
          directChild(xml, SerifXML.FLEXIBLE_EVENT_MENTION_SET_ELEMENT);
      if (flexibleEventMentionSetElement.isPresent()) {
        builder.flexibleEventMentions(
            toFlexibleEventMentions(flexibleEventMentionSetElement.get(), tokenSequences));
      } else {
        builder.flexibleEventMentions(FlexibleEventMentions.absent());
      }

      final Optional<Element> acronymSetElement = directChild(xml, "AcronymSet");
      if (acronymSetElement.isPresent()) {
        builder.acronyms(toAcronyms(acronymSetElement.get(), tokenSequences));
      }

      final Optional<Element> documentEventArgumentSetElement =
          directChild(xml, DOC_EVENT_ARG_SET_ELEMENT);
      if (documentEventArgumentSetElement.isPresent()) {
        builder
            .documentEventArguments(toDocumentEventArguments(documentEventArgumentSetElement.get(),
                document.originalText()));
      } else {
        builder.documentEventArguments(DocumentEventArguments.absent());
      }

      final Optional<Element> documentEventsElement = directChild(xml, DOC_EVENT_SET_ELEMENT);
      if (documentEventsElement.isPresent()) {
        builder.documentEvents(
            toDocumentEvents(documentEventsElement.get(), document.originalText()));
      } else {
        builder.documentEvents(DocumentEvents.absent());
      }

      final DocTheory ret = builder.build();

      // seal all value mentions
      for (final ValueMention valueMention : ret.valueMentions()) {
        valueMention.seal();
      }
      for (final SentenceTheory st : ret.nonEmptySentenceTheories()) {
        for (final ValueMention vm : st.valueMentions()) {
          vm.seal();
        }
      }
      return ret;
    }

    private void loadAlgorithms(final Element algEl) {
      final Optional<Element> morphAlgEl = directChild(algEl, "MorphologyAlgorithms");
      if (morphAlgEl.isPresent()) {
        loadMorphAlgorithms(morphAlgEl.get());
      }
    }

    private void loadMorphAlgorithms(final Element algsEl) {
      for (final Element algEl : childrenWithTag(algsEl, "MorphologyAlgorithm")) {
        record(new MorphologyAlgorithmDescription.Builder()
            .name(requiredSymbolAttribute(algEl, "name"))
            .providesMorphs(optionalBooleanAttribute(algEl, "morphs").or(false))
            .providesFeatures(optionalBooleanAttribute(algEl, "features").or(false))
            .providesLemmas(optionalBooleanAttribute(algEl, "lemmas").or(false))
            .providesRoots(optionalBooleanAttribute(algEl, "roots").or(false))
            .providesSequenceScores(optionalBooleanAttribute(algEl, "sequence_scores").or(false))
            .providesTokenScores(optionalBooleanAttribute(algEl, "token_scores").or(false))
            .glossesLemmas(optionalBooleanAttribute(algEl, "glosses_lemmas").or(false))
            .glossesRoots(optionalBooleanAttribute(algEl, "glosses_roots").or(false))
            .hasLexicon(optionalBooleanAttribute(algEl, "has_lexicon").or(false))
            .build(), algEl);
      }
    }

    /**
     * Loads up objects stored at the document level which will be referred to by sentence-level
     * morphology theories.  These are stored here because they are likely to be repeated across
     * a document.
     */
    private void loadMorphologyInformation(final Element root) {
      for (int i = 0; i < root.getChildNodes().getLength(); ++i) {
        final Node morphNode = root.getChildNodes().item(i);
        if (morphNode instanceof Element) {
          final Element morphEl = (Element) morphNode;
          switch (morphEl.getTagName()) {
            case "Gloss":
              record(Gloss.of(morphEl.getTextContent()), morphEl);
              break;
            case "LexicalForm":
              record(new LexicalForm.Builder()
                  .form(Symbol.from(morphEl.getAttribute("form")))
                  .inLexicon(optionalBooleanAttribute(morphEl, "in_lexicon").or(false))
                  .glosses(this.<Gloss>fetchListOrEmpty("glosses", morphEl)).build(), morphEl);
              break;
            case "Morph":
              final Morph.Builder morphBuilder = new Morph.Builder()
                  .form(requiredSymbolAttribute(morphEl, "form"))
                  .morphType(MorphType.of(morphEl.getAttribute("type")));

              if (morphEl.hasAttribute("features")) {
                for (final String feature : StringUtils.onCommas()
                    .split(morphEl.getAttribute("features"))) {
                  morphBuilder.addFeatures(MorphFeature.of(feature));
                }
              }

              record(morphBuilder.build(), morphEl);
              break;
            case "MorphTokenAnalysis":
              final MorphTokenAnalysis.Builder analysisB = new MorphTokenAnalysis.Builder()
                  .sourceAlgorithm(
                      this.<MorphologyAlgorithmDescription>fetch("algorithm_id", morphEl))
                  .score(optionalDoubleAttribute(morphEl, "score"))
                  .lemmas(this.<LexicalForm>fetchListOrEmpty("lemmas", morphEl))
                  .roots(this.<LexicalForm>fetchListOrEmpty("roots", morphEl))
                  .morphs(this.<Morph>fetchListOrEmpty("morphs", morphEl));

              if (morphEl.hasAttribute("features")) {
                for (final String feature : StringUtils.onCommas()
                    .split(morphEl.getAttribute("features"))) {
                  analysisB.addMorphFeatures(MorphFeature.of(feature));
                }
              }
              record(analysisB.build(), morphEl);
              break;
          }
        }
      }
    }

    private DateTime parseDateTime(final String text) {
      try {
        return formatter1.withOffsetParsed().parseDateTime(text);
      } catch (IllegalArgumentException e1) {
        try {
          return formatter2.parseLocalDateTime(text).toDateTime();
        } catch (IllegalInstantException e) { // Invalid time likely due to daylight savings
          return formatter2.parseLocalDateTime(text).toLocalDate().toDateTimeAtStartOfDay();
        } catch (IllegalArgumentException e2) {
          try {

            return formatter3.parseLocalDateTime(text).toDateTime();
          } catch (IllegalInstantException e) { // Invalid time likely due to daylight savings
            return formatter3.parseLocalDateTime(text).toLocalDate().toDateTimeAtStartOfDay();
          } catch (IllegalArgumentException e3) {
            try {
              return formatter4.parseLocalDateTime(text).toDateTime();
            } catch (IllegalInstantException e) { // Invalid time likely due to daylight savings
              return formatter4.parseLocalDateTime(text).toLocalDate().toDateTimeAtStartOfDay();
            }
          }
        }
      }
    }

    private Document toDocument(final Element e) {
      checkArgument(e.getTagName().equalsIgnoreCase("Document"));

      final Document.Builder retB = new Document.Builder();

      retB.name(Symbol.from(requiredAttribute(e, "docid")));

      final String sourceTypeString = requiredAttribute(e, "source_type");
      if (!"UNKNOWN".equals(sourceTypeString)) {
        retB.sourceType(Symbol.from(sourceTypeString));
      }

      LocatedString originalText = null;
      retB.language(languageLookupMap().languageFor(
          requiredAttribute(e, "language")));
      retB.url(optionalStringAttribute(e, "URL"));
      Optional<String> documentStartTime = optionalStringAttribute(e, "document_time_start");
      if (documentStartTime.isPresent()) {
        final String documentEndTime = requiredSymbolAttribute(e, "document_time_end").toString();
        final DateTime start = parseDateTime(documentStartTime.get());
        final DateTime end = parseDateTime(documentEndTime);
        retB.jodaDocumentTimeInterval(new Interval(start, end));
      }
      final Optional<OffsetGroup> documentOffsetIntoSource = namedOffsetGroupForElement(e, OFFSET_INTO_SOURCE);
      retB.offsetIntoSource(documentOffsetIntoSource);

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (childElement.getTagName().equalsIgnoreCase("Metadata")) {
            retB.metadata(toMetadata(childElement));
          } else if (childElement.getTagName().equalsIgnoreCase("Regions")) {
            // OriginalText will have been loaded before Regions, in case we need to reference it
            retB.regions(toRegions(childElement, originalText));
          } else if (childElement.getTagName().equalsIgnoreCase("Zones")) {
            // OriginalText will have been loaded before Zones, in case we need to reference it
            retB.zoning(toZoning(childElement, originalText));
          } else if (childElement.getTagName().equalsIgnoreCase("OriginalText")) {
            setOffsetIntoSourceDocument(retB, documentOffsetIntoSource, childElement);
            retB.originalText(originalText = toLocatedString(childElement, null));
            retB.url(childElement.getAttribute("href"));
          } else if (childElement.getTagName().equalsIgnoreCase("Segments")) {
            retB.segments(toSegments(childElement));
          } else if (childElement.getTagName().equalsIgnoreCase("DateTime")) {
            retB.dateTimeField(toLocatedString(childElement, originalText));
          }
        }
      }

      final Document ret = retB.build();
      record(ret, e);
      return ret;
    }

    private void setOffsetIntoSourceDocument(final Document.Builder retB,
        final Optional<OffsetGroup> documentOffsetIntoSource, final Element childElement) {
      if (compatibilityWithDocumentsOffsetIntoSource()) {
        // compatibility mode reading happens first to enable handling odd documents and setting the offset global variable
        final OffsetGroupRange offsets = offsetsForElement(childElement);
        if(offsets.startCharOffsetInclusive().asInt() > 0) {
          this.sourceDocumentOffsetShift = Optional.of(offsets.startInclusive());
          if(documentOffsetIntoSource.isPresent()) {
            throw new SerifException(
                "Cannot be offset into source and be modifying source offsets for compatibility, but have serialized source offset "
                    + documentOffsetIntoSource + " and non-zero offset for text " + offsets);
          }
          retB.offsetIntoSource(offsets.startInclusive());
        } else {
          this.sourceDocumentOffsetShift = Optional.absent();
        }
      }
    }

    private Metadata toMetadata(final Element e) {
      checkArgument(e.getTagName().equalsIgnoreCase("Metadata"));
      final ImmutableList.Builder<OffsetGroupSpan> ret = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          ret.add(toSpan((Element) child));
        }
      }
      // don't record - metadata doesn't have an ID
      return Metadata.create(ret.build());
    }

    private OffsetGroupSpan toSpan(final Element e) {
      checkArgument(e.getTagName().equalsIgnoreCase("Span"));
      final OffsetGroupRange offsets = offsetsForElement(e);
      final String spanType = e.getAttribute("span_type");

      final ImmutableSet<String> ignoredAttributeNames = ImmutableSet.of(
          "char_offset", "edt_offset", "id", "span_type");

      final Map<String, String> otherAttributes = new HashMap<>();
      final NamedNodeMap attributes = e.getAttributes();
      for (int i = 0; i < attributes.getLength(); i++) {
        final Node attributeNode = attributes.item(i);
        if (attributeNode instanceof Attr) {
          final String attributeName = attributeNode.getNodeName();
          if (!ignoredAttributeNames.contains(attributeName)) {
            otherAttributes.put(attributeName, attributeNode.getNodeValue());
          }
        }
      }

      final OffsetGroupSpan span =
          OffsetGroupSpan.create(Symbol.from(spanType), offsets, otherAttributes);
      record(span, e);
      return span;
    }

    private List<Region> toRegions(final Element e, final LocatedString originalText) {
      checkArgument(e.getTagName().equalsIgnoreCase("Regions"));
      checkNotNull(originalText);
      final ImmutableList.Builder<Region> ret = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          ret.add(toRegion((Element) child, originalText));
        }
      }

      return ret.build();
    }

    private Region toRegion(final Element e, final LocatedString originalText) {
      final Region region = new Region.Builder()
          .tag(optionalSymbolAttribute(e, "tag"))
          .content(toLocatedString(e, originalText))
          .isSpeakerRegion(optionalBooleanAttribute(e, "is_speaker").or(false))
          .isReceiverRegion(optionalBooleanAttribute(e, "is_receiver").or(false))
          .build();

      record(region, e);
      return region;
    }

    private Zoning toZoning(final Element e, final LocatedString originalText) {
      checkArgument(e.getTagName().equalsIgnoreCase("Zones"));
      checkNotNull(originalText);

      final Element zoningElement = XMLUtils.requiredSingleChild(e, "Zoning");
      final ImmutableList.Builder<Zone> ret = ImmutableList.builder();

      for (Node child = zoningElement.getFirstChild(); child != null;
           child = child.getNextSibling()) {
        if (child instanceof Element) {
          ret.add(toZone((Element) child, originalText));
        }
      }

      return Zoning.create(ret.build());
    }

    private Zone toZone(final Element e, final LocatedString originalText) {
      checkArgument(e.getTagName().equalsIgnoreCase("Zone"));
      final Symbol type = Symbol.from(requiredAttribute(e, "type"));

      final ImmutableList.Builder<Zone> children = ImmutableList.builder();
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element && is((Element) child, "Zone")) {
          children.add(toZone((Element) child, originalText));
        }
      }

      LocatedString author = null;
      LocatedString dateTime = null;
      final ImmutableMap.Builder<Symbol, LocatedString> attributes = ImmutableMap.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element eChild = (Element) child;
          if (is(eChild, "LocatedZoneAttribute")) {
            final Symbol name = Symbol.from(requiredAttribute(child, "name"));
            final LocatedString attributeLS = toLocatedString(eChild, originalText);
            attributes.put(name, attributeLS);
          } else if (is(eChild, "author")) {
            author = toLocatedString(eChild, originalText);
          } else if (is(eChild, "datetime")) {
            dateTime = toLocatedString(eChild, originalText);
          }
        }
      }

      final Zone zone = Zone.create(type, toLocatedString(e, originalText), children.build(),
          attributes.build(), author, dateTime);

      recordIgnoringDuplication(zone, e);
      return zone;
    }

    private LocatedString toLocatedString(final Element e, final LocatedString originalText) {
      final OffsetGroupRange offsets = offsetsForElement(e);

      // Determine the source of our text: a local <Contents>, a file:// URL, or offsets into the document string
      final Optional<Element> contentsChild = directChild(e, "Contents");
      UnicodeFriendlyString text = null;
      if (contentsChild.isPresent()) {
        text = StringUtils.unicodeFriendly(contentsChild.get().getTextContent());
      } else if (e.hasAttribute("href")) {
        final String urlString = requiredAttribute(e, "href");
        if (!urlString.startsWith("file://")) {
          throw new SerifXMLException(
              String.format("Only file:// URLs are supported, but got %s", urlString));
        }
        throw new NotImplementedException("Loading text by URL not yet supported. Copy from C++.");
      }

      final LocatedString ret;
      // Do we have local text or just offsets into the document string?
      if (text == null) {
        if (originalText != null) {
          try {
            // content and reference offsets character must be the same for original text
            // this is enforced when building Document
            ret = originalText
                .contentLocatedSubstringByContentOffsets(OffsetRange.fromInclusiveEndpoints(
                    offsets.startInclusive().charOffset(), offsets.endInclusive().charOffset()));
          } catch (final IndexOutOfBoundsException t) {
            throw new SerifXMLException(String.format(
                "Index out of bounds when loading LocatedString with offsets %s from original text.  Original text is:\n%s\n",
                offsets, originalText.toString()), e, t);
          }
        } else {
          throw new SerifXMLException(
              "Need either a document originalText or a <Contents> child element to load LocatedString");
        }
      } else {
        final UnicodeFriendlyString referenceString;
        if (originalText == null) {
          // this must be the original text
          referenceString = text;
        } else {
          if (originalText.referenceString().isPresent()) {
            referenceString = originalText.referenceString().get();
          } else {
            throw new SerifXMLException("Documents without original text not supported");
          }
        }

        // Read optional <OffsetSpan> children
        List<LocatedString.CharacterRegion> entries = new ArrayList<>();
        try {
          for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && is((Element) child, "OffsetSpan")) {
              final int startPos = Integer.parseInt(requiredAttribute(child, "start_pos"));
              final int endPos = Integer.parseInt(requiredAttribute(child, "end_pos"));
              final OffsetGroupRange spanOffsets = offsetsForElement((Element) child);
              entries
                  .add(new LocatedString.CharacterRegion.Builder()
                      .contentNonBmp(
                          text.hasNonBmpCharacter(
                              OffsetRange.charOffsetRange(startPos, endPos - 1)))
                      .contentStartPosInclusive(CharOffset.asCharOffset(startPos))
                      .contentEndPosExclusive(CharOffset.asCharOffset(endPos))
                      .referenceStartOffsetInclusive(spanOffsets.startInclusive())
                      .referenceEndOffsetInclusive(spanOffsets.endInclusive()).build());
            }
          }

          if (entries.size() == 0) {
            // if there are no offset entries, first ensure EDT = char offsets
            if (offsets.asEdtOffsetRange().length() != offsets.asCharOffsetRange().length()) {
              // if not, we can allow it and throw away the EDT offsets if the user explicitly
              // requests it
              if (allowSloppyOffsets()) {
                warn("No offset entries found; discarding EDT offsets since we can't map "
                    + "them back to text!");
              } else {
                throw new SerifXMLException("Invalid SerifXML document: LocatedStrings lack offset "
                    + "entries.  This was probably produced by an ancient version of CSerif, and you "
                    + "can probably load it using " + ALLOW_SLOPPY_OFFSETS_PARAM
                    + " at the cost of "
                    + "loading potentially incorrect offset information");
              }
            }

            final OffsetGroup startWithoutEDT;
            final OffsetGroup endWithoutEDT;
            // if there are no offset entries, the correspondence between content string positions and
            // reference string character offsets must be one-to-one
            if (offsets.asCharOffsetRange().length() == text.lengthInCodePoints()) {
              startWithoutEDT = OffsetGroup.fromMatchingCharAndEDT(
                  offsets.startInclusive().charOffset().asInt());
              endWithoutEDT = OffsetGroup.fromMatchingCharAndEDT(
                  offsets.endInclusive().charOffset().asInt());
            } else {
              // sometimes this is not true for the Regions in documents produced by old versions of
              // CSerif.  If allowSloppyOffsets is enabled, we will still load these, but Regions
              // will have garbage offsets
              if (allowSloppyOffsets()
                  && TAGS_ALLOWED_TO_HAVE_BAD_OFFSETS_WITH_SLOPPY_OFFSETS
                  .contains(e.getTagName())) {
                warn("Coercing offsets due to " + ALLOW_SLOPPY_OFFSETS_PARAM +
                    ". " + e.getTagName() + " will have nonsense offsets");
                startWithoutEDT = OffsetGroup.fromMatchingCharAndEDT(
                    offsets.startInclusive().charOffset().asInt());
                endWithoutEDT = OffsetGroup.fromMatchingCharAndEDT(
                    offsets.startInclusive().charOffset().asInt() + text.lengthInCodePoints() - 1);
              } else {
                throw new SerifXMLException("Mismatch between reference and content lengths "
                    + "without offset entries for element " + e.getTagName() + ". You may "
                    + "be able to load this file by specifying " + ALLOW_SLOPPY_OFFSETS_PARAM);
              }
            }

            entries.add(
                new LocatedString.CharacterRegion.Builder()
                    .contentNonBmp(text.hasNonBmpCharacter(
                        OffsetRange.charOffsetRange(0, text.lengthInCodePoints() - 1)))
                    .contentStartPosInclusive(CharOffset.asCharOffset(0))
                    .contentEndPosExclusive(CharOffset.asCharOffset(text.lengthInCodePoints()))
                    .referenceStartOffsetInclusive(startWithoutEDT)
                    .referenceEndOffsetInclusive(endWithoutEDT).build());
          }
          ret = new LocatedString.Builder()
              .referenceString(referenceString)
              .content(text)
              .characterRegions(entries)
              .build();
        } catch (IllegalArgumentException iae) {
          if (allowSloppyOffsets()) {
            warn("Offset problem when loading LocatedString: " + iae);
            return new LocatedString.Builder().content(text)
                .referenceString(referenceString).characterRegions(ImmutableList.of(
                    new LocatedString.CharacterRegion.Builder()
                        .contentNonBmp(text.hasNonBmpCharacter(
                            OffsetRange.charOffsetRange(0, text.lengthInCodePoints() - 1)))
                        .contentStartPosInclusive(CharOffset.asCharOffset(0))
                        .contentEndPosExclusive(CharOffset.asCharOffset(text.lengthInCodePoints()))
                        .referenceStartOffsetInclusive(offsets.startInclusive())
                        .referenceEndOffsetInclusive(offsets.endInclusive()).build())).build();
          } else {
            throw iae;
          }
        }
      }

      return ret;
    }

    private List<Segment> toSegments(final Element e) {
      if (hasAnyChildElement(e)) {
        warn("Don't know how to parse Segments yet, copy from C++!");
        //throw new NotImplementedException("Don't know how to parse Segments yet, copy from C++!");
      }
      return ImmutableList.of();
    }

    private List<TokenSequence> gatherTokenSequences(final Element xml,
        final LocatedString originalText) {
      final ImmutableList.Builder<TokenSequence> ret = ImmutableList.builder();

      final Optional<Element> sentencesElement = directChild(xml, "Sentences");
      if (sentencesElement.isPresent()) {
        int sentenceIdx = 0;
        for (Node child = sentencesElement.get().getFirstChild(); child != null;
             child = child.getNextSibling()) {
          if (child instanceof Element) {
            final Element childElement = (Element) child;
            if (!is(childElement, "Sentence")) {
              throw new SerifXMLException("Only sentence elements should appear under Sentences");
            }
            for (Node sentenceChild = child.getFirstChild(); sentenceChild != null;
                 sentenceChild = sentenceChild.getNextSibling()) {
              if (sentenceChild instanceof Element) {
                final Element sentenceChildElement = (Element) sentenceChild;
                if (is(sentenceChildElement, "TokenSequence")) {
                  ret.add(toTokenSequence(sentenceChildElement, originalText, sentenceIdx));
                }
              }
            }
            ++sentenceIdx;
          }
        }
      }

      return ret.build();
    }

    private SentenceTheoryBeam toSentenceTheoryBeam(final Element e, final int sentenceIdx,
        final Document document) {
      checkArgument(e.getTagName().equals("Sentence"));
      boolean hasPOSSequence = false;
      final Sentence sentence = Sentence.forSentenceInDocument(document, sentenceIdx)
          .region(this.<Region>fetch(e.getAttribute("region_id")))
          .locatedString(toLocatedString(e, document.originalText()))
          .build();

      final List<SentenceTheory> sentenceTheories = new ArrayList<>();
      // this is just to make debugging more convenient
      Parse lastParse = null;
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "NameTheory")) {
            toNameTheory(childElement);
          } else if (is(childElement, "NestedNameTheory")) {
            toNestedNameTheory(childElement);
          } else if (is(childElement, "PartOfSpeechSequence")) {
            hasPOSSequence = true;
          } else if (is(childElement, "MorphTokenSequence")) {
            toMorphTokenSequences(childElement);
          } else if (is(childElement, "ValueMentionSet")) {
            toValueMentionSet(childElement);
          } else if (is(childElement, "Parse")) {
            lastParse = toParse(childElement);
          } else if (is(childElement, "NPChunkTheory")) {
            for (Node n = childElement.getFirstChild(); n != null; n = n.getNextSibling()) {
              if (n instanceof Element) {
                final Element nElement = (Element) n;
                if (is(nElement, "Parse")) {
                  toParse(nElement);
                }
              }
            }
          } else if (is(childElement, "MentionSet")) {
            toMentionSet(childElement);
          } else if (is(childElement, "RelMentionSet")) {
            toRelationMentions(childElement);
          } else if (is(childElement, "EventMentionSet")) {
            toEventMentions(childElement, Optional.fromNullable(lastParse));
          } else if (is(childElement, "PropositionSet")) {
            toPropositions(childElement);
          } else if (is(childElement, "DependencySet")) {
            toDependencies(childElement);
          }else if (is(childElement, "ActorMentionSet")) {
            toActorMentions(childElement);
          } else if (is(childElement, "SentenceTheory")) {
            sentenceTheories
                .add(toSentenceTheory(childElement, sentenceIdx, sentence, hasPOSSequence));
          }
        }
      }

      final SentenceTheoryBeam sentenceTheoryBeam = SentenceTheoryBeam.forSentenceTheories(
          sentence, sentenceTheories);
      record(sentenceTheoryBeam, e);
      return sentenceTheoryBeam;
    }

    private void toMorphTokenSequences(final Element root) {
      final TokenSequence tokSeq = fetch("tok_seq_id", root);
      final MorphTokenSequence.Builder morphTokSeqB = new MorphTokenSequence.Builder()
          .sourceAlgorithm(this.<MorphologyAlgorithmDescription>fetch("algorithm_id", root))
          .tokenSequence(tokSeq)
          .score(optionalDoubleAttribute(root, "score"));

      for (final Element morphTokEl : childrenWithTag(root, "MorphToken")) {
        morphTokSeqB.addMorphTokens(new MorphToken.Builder()
            .span(parseTokenSpan(requiredAttribute(morphTokEl, "span"), tokSeq))
            .addAllAnalyses(this.<MorphTokenAnalysis>fetchListOrEmpty("analyses", morphTokEl))
            .build());
      }

      record(morphTokSeqB.build(), root);
    }

    private TokenSequence.Span parseTokenSpan(final String colonPair, final TokenSequence tokSeq) {
      final List<String> parts = ON_COLON.splitToList(colonPair);
      if (parts.size() == 2) {
        return tokSeq.span(Integer.parseInt(parts.get(0)), Integer.parseInt(parts.get(1)));
      } else {
        throw new SerifXMLException("Invalid token span string " + colonPair);
      }
    }

    private SentenceTheory toSentenceTheory(final Element element, final int sentenceIdx,
        final Sentence sentence, final boolean hasPOSSequence) {

      final TokenSequence tokenSequence;
      if (element.hasAttribute("token_sequence_id")) {
        tokenSequence = fetch("token_sequence_id", element);
      } else {
        tokenSequence = TokenSequence.absent(sentenceIdx);
      }

      SentenceTheory.Builder stBuilder =
          SentenceTheory.createForTokenSequence(sentence, tokenSequence);

      if (element.hasAttribute("event_mention_set_id")) {
        stBuilder = stBuilder.eventMentions(
            (EventMentions) (fetch("event_mention_set_id", element)));
      }

      if (element.hasAttribute("morph_tok_seq_ids")) {
        stBuilder.morphTokenSequences(
            this.<MorphTokenSequence>fetchListOrEmpty("morph_tok_seq_ids", element));
      }

      if (element.hasAttribute("mention_set_id")) {
        stBuilder = stBuilder.mentions((Mentions) fetch("mention_set_id", element));
      }

      if (element.hasAttribute("name_theory_id")) {
        stBuilder = stBuilder.withNameTheory((Names) fetch("name_theory_id", element));
      }

      if (element.hasAttribute("nested_name_theory_id")) {
        stBuilder =
            stBuilder.nestedNames((NestedNames) fetch("nested_name_theory_id", element));
      }

      // check whether the primary_parse is full_parse or npchunk_parse
      if (element.hasAttribute("primary_parse")) {
        final String parseType = element.getAttribute("primary_parse");
        if (parseType.compareTo("npchunk_parse") == 0) {
          if (element.hasAttribute("np_chunk_theory_id")) {
            final String id = requiredAttribute(element, "np_chunk_theory_id");
            final int idIndex = Integer.parseInt(id.substring(1)) + 1;
            stBuilder = stBuilder.parse((Parse) fetch("a" + idIndex));
          }
        } else if (parseType.compareTo("full_parse") == 0) {
          if (element.hasAttribute("parse_id")) {
            final Parse parse = fetch("parse_id", element);
            stBuilder = stBuilder.parse(parse);
          }
        }
      }

      if (element.hasAttribute("proposition_set_id")) {
        stBuilder = stBuilder.propositions((Propositions) fetch("proposition_set_id", element));
      }

      if (element.hasAttribute("dependency_set_id")) {
        stBuilder = stBuilder.dependencies((Dependencies) fetch("dependency_set_id", element));
      }

      if (element.hasAttribute("rel_mention_set_id")) {
        stBuilder =
            stBuilder.relationMentions((RelationMentions) fetch("rel_mention_set_id", element));
      }

      if (element.hasAttribute("value_mention_set_id")) {
        stBuilder =
            stBuilder.valueMentions((ValueMentions) fetch("value_mention_set_id", element));
      }

      if (element.hasAttribute("actor_mention_set_id")) {
        stBuilder =
            stBuilder.actorMentions((ActorMentions) fetch("actor_mention_set_id", element));
      }

      if (hasPOSSequence) {
        stBuilder = stBuilder.hasPOSSequence(true);
      }

      final SentenceTheory st = stBuilder.build();

      record(st, element);
      return st;
    }

    private TokenSequence toTokenSequence(final Element e, LocatedString originalText,
        final int sentenceIdx) {
      checkArgument(e.getTagName().equals("TokenSequence"));

      final TokenSequence.FromTokenDataBuilder builder;
      builder = TokenSequence.withOriginalText(sentenceIdx, originalText);
      builder.setScore(requiredFloatAttribute(e, "score"));

      // we need this to record the token-element mapping after the TokenSequence is created
      final List<Element> tokenElements = Lists.newArrayList();
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = ((Element) child);
          checkState(is(childElement, "Token"));
          final OffsetGroupRange offsets = offsetsForElement(childElement);
          builder.addToken(Symbol.from(childElement.getTextContent()), offsets.asCharOffsetRange());
          tokenElements.add(childElement);
        }
      }
      final TokenSequence ts = builder.build();
      record(ts, e);
      for (int i = 0; i < ts.size(); ++i) {
        record(ts.token(i), tokenElements.get(i));
      }
      return ts;
    }

    private Names toNameTheory(final Element e) {
      checkNotNull(e);
      final TokenSequence ts = fetch("token_sequence_id", e);
      checkNotNull(ts);
      final Double score = optionalDoubleAttribute(e, "score").orNull();

      final ImmutableList.Builder<Name> names = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (childElement.getTagName().equals("Name")) {
            names.add(toName(childElement, ts));
          }
        }
      }

      final Names nameTheory = Names.createFrom(names.build(), ts, score);
      record(nameTheory, e);
      return nameTheory;
    }

    private NestedNames toNestedNameTheory(final Element e) {
      checkNotNull(e);
      final TokenSequence ts = fetch("token_sequence_id", e);
      checkNotNull(ts);
      final Names names = fetch("name_theory_id", e);
      checkNotNull(names);

      final Optional<Double> score = optionalDoubleAttribute(e, "score");

      final ImmutableList.Builder<NestedName> nestedNames = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (childElement.getTagName().equals("NestedName")) {
            nestedNames.add(toNestedNameTheory(childElement, ts));
          }
        }
      }

      final NestedNames nested =
          new NestedNames.Builder().tokenSequence(ts).parent(names).names(nestedNames.build())
              .score(score).build();
      record(nested, e);
      return nested;
    }

    private Map<String, Element> buildPropIDsToElementsMap(final Element e) {
      final ImmutableMap.Builder<String, Element> propIDsToElements = ImmutableMap.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Proposition")) {
            propIDsToElements.put(requiredAttribute(childElement, "id"), childElement);
          }
        }
      }

      return propIDsToElements.build();
    }

    private Propositions toPropositions(final Element e) {
      final Mentions mentions = fetch("mention_set_id", e);
      final ImmutableList.Builder<Proposition> propositions = ImmutableList.builder();
      // building propositions is a bit complicated because, unlike almost everything else,
      // proposition arguments can refer to propositions which haven't been created yet.  So
      // we need to first build a map from Proposition IDs to XML elements so we can make sure
      // we can createWithMentions a requested proposition when it is demanded if we haven't already

      final Map<String, Element> propIDsToElements = buildPropIDsToElementsMap(e);

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Proposition")) {
            final Proposition prop = elementToProposition(childElement, propIDsToElements);
            propositions.add(prop);
          }
        }
      }

      final Propositions ret = new Propositions.Builder()
          .propositions(propositions.build())
          .mentions(mentions).build();
      record(ret, e);
      return ret;
    }

    private Dependencies toDependencies(final Element e) {
      final Mentions mentions = fetch("mention_set_id", e);
      final ImmutableList.Builder<Proposition> propositions = ImmutableList.builder();
      // building propositions is a bit complicated because, unlike almost everything else,
      // proposition arguments can refer to propositions which haven't been created yet.  So
      // we need to first build a map from Proposition IDs to XML elements so we can make sure
      // we can createWithMentions a requested proposition when it is demanded if we haven't already

      final Map<String, Element> propIDsToElements = buildPropIDsToElementsMap(e);

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Proposition")) {
            final Proposition prop = elementToProposition(childElement, propIDsToElements);
            propositions.add(prop);
          }
        }
      }

      final Dependencies ret = new Dependencies.Builder()
          .propositions(propositions.build())
          .mentions(mentions).build();
      record(ret, e);
      return ret;
    }

    private Relations toRelations(final Element e) {
      final ImmutableList.Builder<Relation> relations = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Relation")) {
            final float confidence = requiredFloatAttribute(childElement, "confidence");
            final Entity leftEntity = fetch("left_entity_id", childElement);
            final Entity rightEntity = fetch("right_entity_id", childElement);
            final Modality modality =
                Modality.from(requiredSymbolAttribute(childElement, "modality"));
            final List<String> relationMentionIDs =
                requiredStringList(childElement, "rel_mention_ids",
                    Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings());
            final Symbol type = requiredSymbolAttribute(childElement, "type");
            final Tense tense = Tense.from(requiredSymbolAttribute(childElement, "tense"));
            final Symbol external_id = getExternalID(childElement).orNull();

            final List<RelationMention> relMentions = Lists.newArrayList();
            for (final String relMentionId : relationMentionIDs) {
              relMentions.add(this.<RelationMention>fetch(relMentionId));
            }

            final Relation rel = Relation.create(leftEntity, rightEntity,
                new RelationMentions.Builder().relationMentions(relMentions).build(),
                type, tense, modality, confidence, external_id);
            record(rel, childElement);
            relations.add(rel);
          }
        }
      }

      return Relations.create(relations.build());
    }

    private Events toEvents(final Element e) {
      final ImmutableList.Builder<Event> events = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Event")) {
            final Symbol type = requiredSymbolAttribute(childElement, "event_type");
            final Genericity genericity =
                Genericity.from(requiredSymbolAttribute(childElement, "genericity"));
            final Modality modality =
                Modality.from(requiredSymbolAttribute(childElement, "modality"));
            final Tense tense = Tense.from(requiredSymbolAttribute(childElement, "tense"));
            final Polarity polarity =
                Polarity.from(requiredSymbolAttribute(childElement, "polarity"));
            final List<String> eventIDs = requiredStringList(childElement, "event_mention_ids",
                Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings());
            final List<EventMention> mentions = Lists.newArrayList();
            final Symbol external_id = getExternalID(childElement).orNull();

            for (final String eventID : eventIDs) {
              mentions.add(this.<EventMention>fetch(eventID));
            }

            final List<Event.Argument> arguments = Lists.newArrayList();

            for (Node argChild = childElement.getFirstChild(); argChild != null;
                 argChild = argChild.getNextSibling()) {
              if (argChild instanceof Element) {
                final Element argElement = (Element) argChild;
                if (is(argElement, "EventArg")) {
                  final Symbol role = requiredSymbolAttribute(argElement, "role");
                  final Entity entity = this.fetchFromOptionalAttribute("entity_id", argElement);
                  final Value value = this.fetchFromOptionalAttribute("value_id", argElement);

                  if (entity != null) {
                    arguments.add(new Event.EntityArgument(entity, role));
                  } else if (value != null) {
                    arguments.add(new Event.ValueArgument(value, role));
                  } else {
                    throw new SerifXMLException(String.format(
                        "Event argument has neither entity nor value: %s; parent is %s",
                        dumpElement(argElement), dumpElement(childElement)));
                  }
                }
              }
            }

            final Event event =
                new Event(arguments, new EventMentions.Builder().eventMentions(mentions).build(),
                    type, genericity, modality,
                    polarity, tense, external_id);
            record(event, childElement);
            events.add(event);
          }
        }
      }

      return new Events(events.build());
    }

    private EventEventRelationMentions toEventEventRelationMentions(final Element e) {
      final ImmutableList.Builder<EventEventRelationMention> eventEventRelationMentions =
          ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "EventEventRelationMention")) {
            final Symbol relationType = requiredSymbolAttribute(
                childElement, "relation_type");
            final Optional<Double> confidence = optionalDoubleAttribute(
                childElement, "confidence");
            final Optional<String> pattern = optionalStringAttribute(
                childElement, "pattern");
            final Optional<String> model = optionalStringAttribute(
                childElement, "model");
            final Optional<String> triggerText = optionalStringAttribute(childElement,"trigger_text");
            Optional<Symbol> polaritySym = optionalSymbolAttribute(childElement,"polarity");
            Optional<Polarity> polarity = Optional.absent();
            if(polaritySym.isPresent()){
              polarity = Optional.of(Polarity.from(polaritySym.get()));
            }

            EventEventRelationMention.Argument leftArg = null;
            EventEventRelationMention.Argument rightArg = null;

            for (Node argChild = childElement.getFirstChild(); argChild != null;
                 argChild = argChild.getNextSibling()) {
              if (!(argChild instanceof Element)) {
                continue;
              }
              if (leftArg == null)
                leftArg = toRelationArgument((Element) argChild);
              else
                rightArg = toRelationArgument((Element) argChild);
            }

            final EventEventRelationMention eerm = new EventEventRelationMention.Builder()
                .relationType(relationType)
                .leftEventMention(leftArg)
                .rightEventMention(rightArg)
                .confidence(confidence)
                .pattern(pattern)
                .model(model)
                .polarity(polarity)
                .triggerText(triggerText)
                .build();

            record(eerm, childElement);
            eventEventRelationMentions.add(eerm);
          }
        }
      }

      return EventEventRelationMentions.create(eventEventRelationMentions.build());
    }

    private EventEventRelationMention.Argument toRelationArgument(final Element e) {
      Symbol role = requiredSymbolAttribute(e, "role");
      if (is(e, "EventMentionRelationArgument")) {
        final EventMention eventMention = this.fetch("event_mention_id", e);
        return new EventEventRelationMention.EventMentionArgument(eventMention, role);
      } else if (is(e, "ICEWSEventMentionRelationArgument")) {
        final ICEWSEventMention icewsEventMention = this.fetch("icews_event_mention_id", e);
        return new EventEventRelationMention.ICEWSEventMentionArgument(icewsEventMention, role);
      }
      throw new SerifXMLException(
          "Unrecognized child element of an EventEventRelationMention: " + e.toString());
    }

    private ActorEntities toActorEntities(final Element e) {
      final ImmutableList.Builder<ActorEntity> actorEntities = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "ActorEntity")) {
            final double confidence = requiredDoubleAttribute(childElement, "confidence");
            Entity entity = this.fetch(requiredAttribute(childElement, "entity_id"));
            final Symbol sourceNote = optionalSymbolAttribute(childElement, "source_note").orNull();
            final Long actorID = optionalLongAttribute(childElement, "actor_uid").orNull();
            final List<Symbol> actorMentionIDs =
                optionalSymbolList(childElement, "actor_mention_ids",
                    Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults());
            final List<ActorMention> mentions = Lists.newArrayList();
            for (final Symbol mentionID : actorMentionIDs) {
              mentions.add(this.<ActorMention>fetch(mentionID.toString()));
            }
            final Symbol actorName;
            final String actorNameString = childElement.getTextContent();
            if (actorNameString != null && actorNameString.length() > 0) {
              actorName = Symbol.from(actorNameString);
            } else {
              actorName = requiredSymbolAttribute(childElement, "actor_name");
            }
            final Optional<Symbol> actorDBName =
                optionalSymbolAttribute(childElement, "actor_db_name");
            final ActorEntity actorEntity = ActorEntity
                .createWithMentions(Optional.fromNullable(actorID), actorName, mentions,
                    entity, confidence, sourceNote, actorDBName);
            record(actorEntity, childElement);
            actorEntities.add(actorEntity);
          }
        }
      }

      return ActorEntities.create(actorEntities.build());
    }

    private Facts toFacts(final Element e, List<TokenSequence> tokenSequences) {
      final ImmutableList.Builder<Fact> facts = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Fact")) {
            final int startSentence = requiredIntegerAttribute(childElement, "start_sentence");
            final int endSentence = requiredIntegerAttribute(childElement, "end_sentence");
            final int startToken = requiredIntegerAttribute(childElement, "start_token");
            final int endToken = requiredIntegerAttribute(childElement, "end_token");
            final double score = requiredDoubleAttribute(childElement, "score");
            final Symbol factType = requiredSymbolAttribute(childElement, "fact_type");
            final Integer scoreGroup =
                optionalIntegerAttribute(childElement, "score_group").orNull();
            List<Fact.Argument> arguments = Lists.newArrayList();
            for (Node argChild = childElement.getFirstChild(); argChild != null;
                 argChild = argChild.getNextSibling()) {
              if (!(argChild instanceof Element)) {
                continue;
              }
              arguments.add(toFactArgument((Element) argChild, tokenSequences));
            }
            final TokenSpan factSpan =
                TokenSpans.from(tokenSequences.get(startSentence).token(startToken))
                    .through(tokenSequences.get(endSentence).token(endToken));

            final Fact fact =
                Fact.create(factType, arguments, score, Optional.fromNullable(scoreGroup),
                    factSpan);
            // nothing yet refers to Facts, so we don't need to record them
            // Also, there are SerifXML documents with id-less Facts floating
            // around ~ rgabbard
            //record(fact,childElement);
            facts.add(fact);
          }
        }
      }
      return Facts.create(facts.build());
    }

    private Fact.Argument toFactArgument(final Element argChildElement,
        final List<TokenSequence> tokenSequences) {
      Symbol role = requiredSymbolAttribute(argChildElement, "role");
      if (is(argChildElement, "MentionFactArgument")) {
        final Mention mention = this.fetch("mention_id", argChildElement);
        return Fact.MentionArgument.create(role, mention);
      } else if (is(argChildElement, "ValueMentionFactArgument")) {
        final boolean isDocDate = requiredBooleanAttribute(argChildElement, "is_doc_date");
        ValueMention valueMention = this.fetchFromOptionalAttribute("value_mention_id",
            argChildElement);
        return Fact.ValueMentionArgument.create(role, valueMention, isDocDate);
      } else if (is(argChildElement, "TextSpanFactArgument")) {
        final int argStartSentence =
            requiredIntegerAttribute(argChildElement, "start_sentence");
        final int argEndSentence = requiredIntegerAttribute(argChildElement, "end_sentence");
        final int argStartToken = requiredIntegerAttribute(argChildElement, "start_token");
        final int argEndToken = requiredIntegerAttribute(argChildElement, "end_token");
        final TokenSpan factArgSpan =
            TokenSpans.from(tokenSequences.get(argStartSentence).token(argStartToken))
                .through(tokenSequences.get(argEndSentence).token(argEndToken));

        return Fact.TextSpanArgument.create(role, factArgSpan);
      } else if (is(argChildElement, "StringFactArgument")) {
        final Symbol string = requiredSymbolAttribute(argChildElement, "string");
        return Fact.StringArgument.create(role, string);
      } else {
        throw new SerifXMLException(
            "Unrecognized child element of a Fact: " + argChildElement.toString());
      }
    }

    private ICEWSEventMentions toICEWSEventMentions(final Element e) {
      final ImmutableList.Builder<ICEWSEventMention> icewsEventMentions = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "ICEWSEventMention")) {
            final Symbol code = requiredSymbolAttribute(childElement, "event_code");
            final Symbol tense = requiredSymbolAttribute(childElement, "event_tense");
            final Symbol patternId = requiredSymbolAttribute(childElement, "pattern_id");

            // Fields for reciprocal events
            // If the XML does not specify, the event is non-reciprocal, so this defaults to false
            final boolean isReciprocal = optionalBooleanAttribute(childElement, "is_reciprocal")
                .or(false);
            // Symbol that tells you the identity of the "original" event that was found by CSerif
            // that led to this particular ICEWSEventMention
            final Optional<Symbol> originalEventId =
                optionalSymbolAttribute(childElement, "original_event_id");

            final Optional<ValueMention> timeValueMention =
                Optional.fromNullable(this.<ValueMention>fetchFromOptionalAttribute(
                    "time_value_mention_id", childElement));

            final List<Symbol> propositionIds =
                optionalSymbolList(childElement, "proposition_ids",
                    Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings().trimResults());
            final List<Proposition> propositions = Lists.newArrayList();
            for (final Symbol propositionId : propositionIds) {
              propositions.add(this.<Proposition>fetch(propositionId.toString()));
            }

            List<ICEWSEventMention.ICEWSEventParticipant> participants = Lists.newArrayList();
            for (Node argChild = childElement.getFirstChild(); argChild != null;
                 argChild = argChild.getNextSibling()) {
              if (!(argChild instanceof Element)) {
                continue;
              }
              final Element argChildElement = (Element) argChild;
              Symbol role = requiredSymbolAttribute(argChildElement, "role");
              final ActorMention actorMention = this.fetch("actor_id", argChildElement);
              final ICEWSEventMention.ICEWSEventParticipant participant =
                  new ICEWSEventMention.ICEWSEventParticipant.Builder()
                      .actorMention(actorMention)
                      .role(role)
                      .build();
              participants.add(participant);
            }

            final ICEWSEventMention icewsEventMention =
                new ICEWSEventMention.Builder()
                    .eventParticipants(participants)
                    .code(code)
                    .tense(tense)
                    .patternId(patternId)
                    .isReciprocal(isReciprocal)
                    .originalEventId(originalEventId)
                    .timeValueMention(timeValueMention)
                    .propositions(propositions)
                    .build();
            record(icewsEventMention, childElement);
            icewsEventMentions.add(icewsEventMention);
          }
        }
      }
      return ICEWSEventMentions.create(icewsEventMentions.build());
    }

    private DocumentActorInfo toDocumentActorInfo(final Element e) {
      final Optional<Element> defaultCountryActorElementOptional =
          directChild(e, "DefaultCountryActor");
      if (!defaultCountryActorElementOptional.isPresent()) {
        throw new SerifXMLException(
            "The DocumentActorInfo element must have a child DefaultCountryActor element.");
      }
      Element defaultCountryActorElement = defaultCountryActorElementOptional.get();
      final long actorId = requiredLongAttribute(defaultCountryActorElement, "actor_uid");
      final DocumentActorInfo documentActorInfo = DocumentActorInfo.create(actorId);
      record(documentActorInfo, e);
      return documentActorInfo;
    }

    private ImmutableList<Acronym> toAcronyms(final Element e,
        final List<TokenSequence> tokenSequences) {
      final ImmutableList.Builder<Acronym> acronyms = ImmutableList.builder();
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Acronym")) {
            Acronym.Provenance acronymProvenance = null;
            Acronym.Provenance expansionProvenance = null;
            for (Node innerChild = child.getFirstChild(); innerChild != null;
                 innerChild = innerChild.getNextSibling()) {
              if (innerChild instanceof Element) {
                final Element innerChildElement = (Element) innerChild;
                if (is(innerChildElement, "Provenance")) {
                  final String type = requiredAttribute(innerChildElement, "type");
                  final Optional<Mention> mention = Optional.fromNullable(
                      this.<Mention>fetchFromOptionalAttribute("mention_id", innerChildElement));

                  final Optional<Integer> startSentence =
                      optionalIntegerAttribute(innerChildElement, "start_sentence");
                  final Optional<Integer> endSentence =
                      optionalIntegerAttribute(innerChildElement, "end_sentence");
                  final Optional<Integer> startToken =
                      optionalIntegerAttribute(innerChildElement, "start_token");
                  final Optional<Integer> endToken =
                      optionalIntegerAttribute(innerChildElement, "end_token");
                  Optional<TokenSpan> tokenSpan = Optional.absent();
                  if (startSentence.isPresent() && endSentence.isPresent() && startToken.isPresent()
                      && endToken.isPresent()) {
                    tokenSpan = Optional
                        .of(TokenSpans
                            .from(tokenSequences.get(startSentence.get()).token(startToken.get()))
                            .through(tokenSequences.get(endSentence.get()).token(endToken.get())));
                  }

                  final Symbol text = requiredSymbolAttribute(innerChildElement, "text");
                  //noinspection IfCanBeSwitch
                  if (type.equals("acronym")) {
                    if (mention.isPresent()) {
                      acronymProvenance = Acronym.createFromMention(mention.get(), text);
                    } else if (tokenSpan.isPresent()) {
                      acronymProvenance = Acronym.createFromTokenSpan(tokenSpan.get(), text);
                    } else {
                      acronymProvenance = Acronym.createFromTextOnly(text);
                    }
                  } else if (type.equals("expansion")) {
                    if (mention.isPresent()) {
                      expansionProvenance = Acronym.createFromMention(mention.get(), text);
                    } else if (tokenSpan.isPresent()) {
                      expansionProvenance = Acronym.createFromTokenSpan(tokenSpan.get(), text);
                    } else {
                      expansionProvenance = Acronym.createFromTextOnly(text);
                    }
                  } else {
                    throw new SerifXMLException(
                        "Unrecognized type: provenance type must be either expansion or acronym");
                  }
                }
              }
            }
            if (acronymProvenance != null) {
              if (expansionProvenance != null) {
                Acronym acronym =
                    Acronym.createExpandedAcronym(acronymProvenance, expansionProvenance);
                acronyms.add(acronym);
              } else {
                Acronym acronym = Acronym.createUnexpandedAcronym(acronymProvenance);
                acronyms.add(acronym);
              }
            } else {
              throw new SerifXMLException(
                  "Acronym element must contain Provenance element of type acronym");
            }
          }
        }
      }
      return acronyms.build();
    }

    private FlexibleEventMentions toFlexibleEventMentions(
        final Element e, final List<TokenSequence> tokenSequences) {
      final ImmutableList.Builder<FlexibleEventMention> flexibleEventMentions =
          ImmutableList.builder();
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, SerifXML.FLEXIBLE_EVENT_MENTION_ELEMENT)) {

            Symbol type = null;
            final ImmutableMap.Builder<Symbol, Symbol> attributeMap = new ImmutableMap.Builder<>();
            NamedNodeMap attributes = childElement.getAttributes();
            for (int index = 0; index < attributes.getLength(); index++) {
              final Symbol name = Symbol.from(attributes.item(index).getNodeName());
              final Symbol value = Symbol.from(attributes.item(index).getNodeValue());
              if (name.equalTo(Symbol.from(SerifXML.EVENT_TYPE_ATTRIBUTE))) {
                type = value;
              } else //noinspection StatementWithEmptyBody
                if (name.equalTo(Symbol.from(SerifXML.GENERIC_ID_ATTRIBUTE))) {
                // "id" is a special attribute used for XML saving/loading, so we do not add it to our object
                } else //noinspection StatementWithEmptyBody
                  if (name.asString().equals(SerifXML.EXTERNAL_ID)) {
                // do nothing, this is not really an attribute
              } else {
                attributeMap.put(name, value);
              }
            }
            if (type == null) {
              throw new SerifXMLException(
                  "type is a required attribute of a FlexibleEventMention Element!");
            }
            final FlexibleEventMention.Builder builder = FlexibleEventMention.builderForType(type)
                .withAttributes(attributeMap.build());

            for (Node argChild = childElement.getFirstChild(); argChild != null;
                 argChild = argChild.getNextSibling()) {
              if (!(argChild instanceof Element)) {
                continue;
              }
              builder
                  .withArgument(toFlexibleEventMentionArgument((Element) argChild, tokenSequences));
            }

            final Optional<Symbol> externalID = getExternalID(childElement);
            if (externalID.isPresent()) {
              builder.withExternalID(externalID.get());
            }
            final FlexibleEventMention flexibleEventMention = builder.build();
            record(flexibleEventMention, childElement);
            flexibleEventMentions.add(flexibleEventMention);
          }
        }
      }
      return FlexibleEventMentions.from(flexibleEventMentions.build());
    }

    private Optional<Symbol> getExternalID(final Element e) {
      return optionalSymbolAttribute(e, SerifXML.EXTERNAL_ID);
    }

    private FlexibleEventMentionArgument toFlexibleEventMentionArgument(
        final Element argChildElement, final List<TokenSequence> tokenSequences) {
      final FlexibleEventMentionArgument flexemArg;
      Symbol role = requiredSymbolAttribute(argChildElement, SerifXML.GENERIC_ROLE_ATTRIBUTE);
      final Optional<Symbol> externalId = getExternalID(argChildElement);
      final Mention mention = fetchFromOptionalAttribute(SerifXML.GENERIC_MENTION_ID_ATTRIBUTE,
          argChildElement);
      final ValueMention valueMention =
          fetchFromOptionalAttribute(SerifXML.GENERIC_VALUE_MENTION_ID_ATTRIBUTE,
              argChildElement);
      final SynNode synNode = fetchFromOptionalAttribute(SerifXML.GENERIC_SYN_NODE_ID_ATTRIBUTE,
          argChildElement);
      //reading geo-resolution, if any present
      final GeoResolvedActor geoResolvedActor;
      if (argChildElement.hasAttribute(SerifXML.GENERIC_GEO_ID_ATTRIBUTE) ||
          argChildElement.hasAttribute(SerifXML.GENERIC_GEO_COUNTRY_ATTRIBUTE)) {
        geoResolvedActor = toGeoResolvedActor(argChildElement);
      } else {
        geoResolvedActor = null;
      }
      //reading temporal-resolution, if any present
      Timex2Time temporalResolution = null;
      for (Node child = argChildElement.getFirstChild(); child != null;
           child = child.getNextSibling()) {
        if (child instanceof Element && is((Element) child, SerifXML.TIMEX2_ELEMENT)) {
          temporalResolution = toTimex2Time((Element) child);
          break;
        }
      }
      if (mention != null) {
        flexemArg = FlexibleEventMentionArgument.from(
            role, mention, Optional.fromNullable(geoResolvedActor));
        return flexemArg.withExternalID(externalId.orNull());
      } else if (valueMention != null) {
        flexemArg =
            FlexibleEventMentionArgument
                .from(role, valueMention, Optional.fromNullable(temporalResolution));
        return flexemArg.withExternalID(externalId.orNull());
      } else if (synNode != null) {
        if (geoResolvedActor != null) {
          flexemArg = FlexibleEventMentionArgument.from(role, synNode, geoResolvedActor);
        } else if (temporalResolution != null) {
          flexemArg = FlexibleEventMentionArgument.from(role, synNode, temporalResolution);
        } else {
          flexemArg = FlexibleEventMentionArgument.from(role, synNode);
        }
        return flexemArg.withExternalID(externalId.orNull());
      } else if (argChildElement.hasAttribute(SerifXML.GENERIC_START_SENTENCE_ATTRIBUTE)
          && argChildElement.hasAttribute(SerifXML.GENERIC_END_SENTENCE_ATTRIBUTE)
          && argChildElement.hasAttribute(SerifXML.GENERIC_START_TOKEN_ATTRIBUTE)
          && argChildElement.hasAttribute(SerifXML.GENERIC_END_TOKEN_ATTRIBUTE)) {
        final int argStartSentence = requiredIntegerAttribute(argChildElement,
            SerifXML.GENERIC_START_SENTENCE_ATTRIBUTE);
        final int argEndSentence = requiredIntegerAttribute(argChildElement,
            SerifXML.GENERIC_END_SENTENCE_ATTRIBUTE);
        final int argStartToken = requiredIntegerAttribute(argChildElement,
            SerifXML.GENERIC_START_TOKEN_ATTRIBUTE);
        final int argEndToken = requiredIntegerAttribute(argChildElement,
            SerifXML.GENERIC_END_TOKEN_ATTRIBUTE);
        final TokenSpan tokenSpan =
            TokenSpans.from(tokenSequences.get(argStartSentence).token(argStartToken))
                .through(tokenSequences.get(argEndSentence).token(argEndToken));
        if (geoResolvedActor != null) {
          flexemArg = FlexibleEventMentionArgument.from(role, tokenSpan, geoResolvedActor);
        } else if (temporalResolution != null) {
          flexemArg = FlexibleEventMentionArgument.from(role, tokenSpan, temporalResolution);
        } else {
          flexemArg = FlexibleEventMentionArgument.from(role, tokenSpan);
        }
        return flexemArg.withExternalID(externalId.orNull());
      } else {
        throw new SerifXMLException(
            "Unrecognized child element of a FlexibleEventMention: " + dumpXMLElement(
                argChildElement));
      }
    }

    private Timex2Time toTimex2Time(final Element e) {

      final Symbol val = requiredSymbolAttribute(e, SerifXML.TIMEX2_VAL_ATTRIBUTE);
      final Symbol mod = nonEmptySymbolOrNull(e, SerifXML.TIMEX2_MOD_ATTRIBUTE);
      final boolean set = optionalBooleanAttribute(e, SerifXML.TIMEX2_SET_ATTRIBUTE).or(false);
      final Symbol granularity = nonEmptySymbolOrNull(e, SerifXML.TIMEX2_GRANULARITY_ATTRIBUTE);
      final Symbol periodicity = nonEmptySymbolOrNull(e, SerifXML.TIMEX2_PERIODICITY_ATTRIBUTE);
      final Symbol anchorVal = nonEmptySymbolOrNull(e, SerifXML.TIMEX2_ANCHOR_VAL_ATTRIBUTE);
      final Symbol anchorDir = nonEmptySymbolOrNull(e, SerifXML.TIMEX2_ANCHOR_DIR_ATTRIBUTE);
      final boolean nonSpecific =
          optionalBooleanAttribute(e, SerifXML.TIMEX2_NON_SPECIFIC_ATTRIBUTE).or(false);

      final Timex2Time.Builder timex2TimeBuilder = Timex2Time.builder().withVal(val);
      if (mod != null) {
        timex2TimeBuilder.withModifierFromString(mod.asString());
      }
      timex2TimeBuilder.withIsSet(set);
      if (granularity != null) {
        timex2TimeBuilder.withGranularity(granularity);
      }
      if (periodicity != null) {
        timex2TimeBuilder.withPeriodicity(periodicity);
      }
      if (anchorVal != null) {
        timex2TimeBuilder.withAnchorValue(anchorVal);
      }
      if (anchorDir != null) {
        timex2TimeBuilder.withAnchorDirection(anchorDir);
      }
      timex2TimeBuilder.setNonSpecific(nonSpecific);
      return timex2TimeBuilder.build();
    }

    private Optional<Proposition> resolvePropositionArgument(final Element e,
        final Map<String, Element> propIDsToElements) {
      checkArgument(is(e, "Argument"));

      final Optional<String> propID = optionalStringAttribute(e, "proposition_id");

      if (!propID.isPresent()) {
        return Optional.absent();
      }

      final Proposition alreadyKnownProp = fetchIfKnownFromRequiredAttribute("proposition_id", e);
      if (alreadyKnownProp != null) {
        return Optional.of(alreadyKnownProp);
      } else {
        return Optional
            .of(elementToProposition(propIDsToElements.get(propID.get()), propIDsToElements));
      }
    }

    private Proposition elementToProposition(final Element childElement,
        final Map<String, Element> propIDsToElements)

    {
      checkNotNull(childElement);
      checkArgument(is(childElement, "Proposition"));

      final Proposition alreadyKnown = fetchIfKnownFromRequiredAttribute("id", childElement);
      if (alreadyKnown != null) {
        return alreadyKnown;
      }

      final Proposition.PredicateType predType =
          Proposition.PredicateType.from(requiredSymbolAttribute(childElement, "type"));
      final SynNode predHead = fetchFromOptionalAttribute("head_id", childElement);
      final SynNode modal = fetchFromOptionalAttribute("modal_id", childElement);
      final SynNode particle = fetchFromOptionalAttribute("particle_id", childElement);
      final SynNode adverb = fetchFromOptionalAttribute("adverb_id", childElement);
      final SynNode negation = fetchFromOptionalAttribute("negation_id", childElement);
      final List<Proposition.Status> statuses =
          Lists.transform(optionalSymbolList(childElement, "status",
              SPACE_SPLITTER), Proposition.Status.FromSymbol);
      final List<Proposition.ArgumentBuilder> args = Lists.newArrayList();

      for (Node argNode = childElement.getFirstChild(); argNode != null;
           argNode = argNode.getNextSibling()) {
        if (argNode instanceof Element) {
          final Element argElement = (Element) argNode;
          if (is(argElement, "Argument")) {
            final Symbol role = optionalSymbolAttribute(argElement, "role").orNull();
            final Mention mention = fetchFromOptionalAttribute("mention_id", argElement);
            final SynNode node = fetchFromOptionalAttribute("syn_node_id", argElement);
            final Optional<Proposition> prop =
                resolvePropositionArgument(argElement, propIDsToElements);

            if (mention != null) {
              args.add(new Proposition.MentionArgumentBuilder(role, mention));
            } else if (node != null) {
              args.add(new Proposition.TextArgumentBuilder(role, node));
            } else if (prop.isPresent()) {
              args.add(new Proposition.PropositionArgumentBuilder(role, prop.get()));
            } else {
              throw new SerifXMLException(String.format(
                  "Proposition argument has neither mention nor SynNode nor proposition: %s; parent is %s",
                  dumpElement(argElement), dumpElement(childElement)));
            }
          }
        }
      }

      final Proposition prop =
          new Proposition(predType, predHead, particle, adverb, negation, modal, args, statuses);
      record(prop, childElement);
      return prop;
    }

    private RelationMentions toRelationMentions(final Element e) {
      final ImmutableList.Builder<RelationMention> relationMentions = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "RelMention")) {
            final RelationMention relMention = new RelationMention.Builder()
                .type(requiredSymbolAttribute(childElement, "type"))
                .leftMention(this.<Mention>fetch("left_mention_id", childElement))
                .rightMention(this.<Mention>fetch("right_mention_id", childElement))
                .timeArg(Optional.fromNullable(
                    this.<ValueMention>fetchFromOptionalAttribute("time_arg_id", childElement)))
                .timeRole(optionalSymbolAttribute(childElement, "time_arg_role"))
                .modality(Modality.from(requiredSymbolAttribute(childElement, "modality")))
                .tense(Tense.from(requiredSymbolAttribute(childElement, "tense")))
                .rawType(optionalSymbolAttribute(childElement, "raw_type"))
                .externalID(getExternalID(childElement))
                .score(optionalDoubleAttribute(childElement, "score").or(1.0))
                    .pattern(optionalStringAttribute(childElement,"pattern"))
                    .model(optionalStringAttribute(childElement,"model"))
                .build();

            record(relMention, childElement);
            relationMentions.add(relMention);
          }
        }
      }

      final RelationMentions ret = new RelationMentions.Builder().
          relationMentions(relationMentions.build()).build();
      recordIgnoringDuplication(ret, e);
      return ret;
    }

    // if genericity or modality scores are absent (which they will be for output from C++
    // SERIF), the scores default to 1.0 ~ rgabbard
    private EventMentions toEventMentions(final Element e, Optional<Parse> lastLoadedParse) {
      Parse parse = fetchFromOptionalAttribute("parse_id", e);
      if (parse == null) {
        // while modern versions of CSerif and JSerif create EventMentionSets with parse_ids,
        // earlier ones did not. For backwards compatibility, when loading an older SerifXML file
        // we will assume the EventMention goes with the last parse loaded. This should always be
        // right if the parse beam size was one.  There are no caches of old
        // documents around with a larger beam size that I am aware of.
        if (lastLoadedParse.isPresent()) {
          warn("EventMentionSet lacks parse_id; using last loaded parse instead");
          parse = lastLoadedParse.get();
        } else {
          throw new SerifException("Cannot have event mentions without parses.");
        }
      }
      final TokenSequence ts = parse.tokenSequence().get();

      // really in the SerifXML the event mentions element records the parse ID of the
      // parse it goes with and we just get the token sequence via that parse. However,
      // this is problematic for empty sentences, which we'd rather represent as Optional.absent
      // In practice we don't use the beam functionality anyway, so we are going to cheat
      // and use the primary token sequence.

      final ImmutableList.Builder<EventMention> eventMentions = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "EventMention")) {
            final Symbol type = requiredSymbolAttribute(childElement, "event_type");
            final Polarity polarity =
                Polarity.from(requiredSymbolAttribute(childElement, "polarity"));
            final Genericity genericity =
                Genericity.from(requiredSymbolAttribute(childElement, "genericity"));
            final Optional<Double> genericityScore =
                optionalDoubleAttribute(childElement, "genericityScore");
            final Modality modality =
                Modality.from(requiredSymbolAttribute(childElement, "modality"));
            final Optional<Double> modalityScore =
                optionalDoubleAttribute(childElement, "modalityScore");

            final Optional<Symbol> gainLossSymbol =
                optionalSymbolAttribute(childElement, "gainLoss");
            final GainLoss gainLoss;
            if (gainLossSymbol.isPresent()) {
              gainLoss = GainLoss.from(gainLossSymbol.get());
            } else {
              gainLoss = null;
            }

            final Optional<Symbol> indicatorSymbol =
                optionalSymbolAttribute(childElement, "indicator");
            final Indicator indicator;
            if (indicatorSymbol.isPresent()) {
              indicator = Indicator.from(indicatorSymbol.get());
            } else {
              indicator = null;
            }

            final Tense tense = Tense.from(requiredSymbolAttribute(childElement, "tense"));

            DirectionOfChange directionOfChange = DirectionOfChange.UNSPECIFIED;
            Optional<Symbol> directionOfChangeOptional = optionalSymbolAttribute(childElement, "direction_of_change");
            if(directionOfChangeOptional.isPresent())
              directionOfChange = DirectionOfChange.from(directionOfChangeOptional.get());

            final Proposition prop = fetchFromOptionalAttribute("anchor_prop_id", childElement);
            final SynNode node = fetchFromOptionalAttribute("anchor_node_id", childElement);
            final Symbol patternID = optionalSymbolAttribute(childElement, "pattern_id").orNull();
            final Symbol external_id = getExternalID(childElement).orNull();
            final double triggerScore = optionalDoubleAttribute(childElement, "score").or(0.0);
            final List<EventMention.Argument> arguments = Lists.newArrayList();

            final Optional<Integer> semanticPhraseStart = optionalIntegerAttribute(childElement, "semantic_phrase_start");
            final Optional<Integer> semanticPhraseEnd = optionalIntegerAttribute(childElement, "semantic_phrase_end");
            final Symbol model = optionalSymbolAttribute(childElement, "model").orNull();
            final List<EventMention.EventType> eventTypes = Lists.newArrayList();
            final List<EventMention.EventType> factorTypes = Lists.newArrayList();
            final List<EventMention.Anchor> anchors = Lists.newArrayList();

            for (Node argNode = childElement.getFirstChild(); argNode != null;
                 argNode = argNode.getNextSibling()) {
              if (argNode instanceof Element) {
                final Element argElement = (Element) argNode;
                if (is(argElement, "EventMentionArg")) {
                  arguments.add(toEventMentionArgument(argElement, ts));
                } else if (is(argElement, "EventMentionType")) {
                  eventTypes.add(toEventMentionType(argElement));
                } else if (is(argElement, "EventMentionFactorType")) {
                  factorTypes.add(toEventMentionFactorType(argElement));
                } else if (is(argElement, "EventMentionAnchor")) {
                  anchors.add(toEventMentionAnchor(argElement));
                }
              }
            }

            final EventMention em =
                new EventMention(type, prop, node, arguments, patternID, modality,
                    modalityScore.or(1.0), polarity, genericity, genericityScore.or(1.0), gainLoss,
                    indicator, tense, directionOfChange, external_id, triggerScore,
                    semanticPhraseStart.orNull(), semanticPhraseEnd.orNull(), model,
                    eventTypes, anchors, factorTypes);
            record(em, childElement);
            eventMentions.add(em);
          }
        }
      }

      final EventMentions ret = new EventMentions.Builder()
          .parse(parse)
          .eventMentions(eventMentions.build())
          .build();
      recordIgnoringDuplication(ret, e);
      return ret;
    }

    private EventMention.EventType toEventMentionType(final Element argElement) {
      final Symbol type = requiredSymbolAttribute(argElement, "event_type");
      final double score = requiredDoubleAttribute(argElement, "score");
      return EventMention.EventType.from(type, score, Optional.<Double>absent(),Optional.<Trend>absent());
    }

    private EventMention.EventType toEventMentionFactorType(final Element argElement) {
      final Symbol type = requiredSymbolAttribute(argElement, "event_type");
      final double score = requiredDoubleAttribute(argElement, "score");
      final Optional<Double> magnitude = optionalDoubleAttribute(argElement, "magnitude");
      Optional<Trend> trend = Optional.absent();
      if(optionalSymbolAttribute(argElement,"trend").isPresent()){
        trend = Optional.of(Trend.from(optionalSymbolAttribute(argElement,"trend").get()));
      }
      return EventMention.EventType.from(type, score, magnitude,trend);
    }

    private EventMention.Anchor toEventMentionAnchor(final Element argElement) {
      final SynNode node = fetchFromOptionalAttribute("anchor_node_id", argElement);
      final Proposition prop = fetchFromOptionalAttribute("anchor_prop_id", argElement);
      return EventMention.Anchor.from(node, prop);
    }

    private EventMention.Argument toEventMentionArgument(final Element argElement,
        final TokenSequence ts) {
      final float score = requiredFloatAttribute(argElement, "score");
      final Symbol role = requiredSymbolAttribute(argElement, "role");

      final Mention mention = fetchFromOptionalAttribute("mention_id", argElement);
      final ValueMention valueMention =
          fetchFromOptionalAttribute("value_mention_id", argElement);
      final Token token = fetchFromOptionalAttribute("start_token", argElement);
      final EventMention.Argument eventArgument;

      if (mention != null) {
        eventArgument = EventMention.MentionArgument.from(role, mention, score);
      } else if (valueMention != null) {
        eventArgument = EventMention.ValueMentionArgument.from(role, valueMention, score);
      } else if (token != null) {
        if (ts == null) {
          throw new SerifXMLException(
              "EventMentionSet lacks parse_id in a document which uses SpanArguments.");
        }
        final TokenSequence.Span span =
            ts.span(token.index(), this.<Token>fetch("end_token", argElement).index());
        eventArgument = EventMention.SpanArgument.from(role, span, score);
      } else {
        throw new SerifXMLException(String
            .format("EventMentionArg is not of a known type. Argument element is %s ",
                dumpXMLElement(argElement)));
      }
      // you need ids on arguments if you want to use them as provenances of document-level
      // event arguments. As of 14 Jan 2016, CSerif doesn't produce them, but JSerif does.
      if (argElement.hasAttribute("id")) {
        record(eventArgument, argElement);
      }
      return eventArgument;
    }

    private ActorMentions toActorMentions(final Element e) {
      final ImmutableList.Builder<ActorMention> actorMentions = ImmutableList.builder();
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "ActorMention")) {
            final Symbol sourceNote = requiredSymbolAttribute(childElement, "source_note");
            final Mention mention = this.fetch(requiredAttribute(childElement, "mention_id"));
            final Optional<Symbol> actorName;
            if (childElement.hasAttribute("actor_name")) {
              actorName = optionalSymbolAttribute(childElement, "actor_name");
            } else if (childElement.getTextContent() != null) {
              actorName = Optional.of(Symbol.from(childElement.getTextContent()));
            } else {
              actorName = Optional.absent();
            }

            final ActorMention am;
            if (childElement.hasAttribute("paired_agent_uid")) {
              final long pairedAgentID = requiredLongAttribute(childElement, "paired_agent_uid");
              final Symbol pairedAgentCode =
                  requiredSymbolAttribute(childElement, "paired_agent_code");
              final Optional<Long> pairedAgentPatternID =
                  optionalLongAttribute(childElement, "paired_agent_pattern_uid");
              final Optional<Symbol> pairedAgentName = optionalSymbolAttribute(childElement,
                  "paired_agent_name");

              final Optional<Long> pairedActorID =
                  optionalLongAttribute(childElement, "paired_actor_uid");
              final Optional<Symbol> pairedActorCode =
                  optionalSymbolAttribute(childElement, "paired_actor_code");
              final Optional<Long> pairedActorPatternID =
                  optionalLongAttribute(childElement, "paired_actor_pattern_uid");
              final Optional<Symbol> pairedActorName = optionalSymbolAttribute(childElement,
                  "paired_actor_name");

              final Optional<Symbol> actorAgentPattern =
                  optionalSymbolAttribute(childElement, "actor_agent_pattern");

              final Optional<Symbol> actorDBName =
                  optionalSymbolAttribute(childElement, "actor_db_name");

              am = CompositeActorMention
                  .create(actorName, sourceNote, pairedAgentID, pairedAgentCode,
                      pairedAgentPatternID, pairedAgentName, pairedActorID, pairedActorCode,
                      pairedActorPatternID,
                      pairedActorName, actorAgentPattern, mention, actorDBName);
            } else if (childElement.hasAttribute("actor_uid")) {
              final Optional<Long> actorPatternID = optionalLongAttribute(childElement,
                  "actor_pattern_uid");
              final long actorID = requiredLongAttribute(childElement, "actor_uid");
              final double associationScore = optionalDoubleAttribute(
                  childElement, "association_score")
                  .or(ProperNounActorMention.DEFAULT_ASSOCIATION_SCORE);
              final double patternConfidenceScore = optionalDoubleAttribute(
                  childElement, "pattern_confidence_score")
                  .or(ProperNounActorMention.DEFAULT_PATTERN_CONFIDENCE_SCORE);
              final double patternMatchScore = optionalDoubleAttribute(
                  childElement, "pattern_match_score")
                  .or(ProperNounActorMention.DEFAULT_PATTERN_MATCH_SCORE);
              final double editDistanceScore = optionalDoubleAttribute(
                  childElement, "edit_distance_score")
                  .or(ProperNounActorMention.DEFAULT_EDIT_DISTANCE_SCORE);
              final double geoResolutionScore = optionalDoubleAttribute(
                  childElement, "georesolution_score")
                  .or(ProperNounActorMention.DEFAULT_GEO_RESOLUTION_SCORE);
              final double importanceScore = optionalDoubleAttribute(
                  childElement, "importance_score")
                  .or(ProperNounActorMention.DEFAULT_IMPORTANCE_SCORE);
              final GeoResolvedActor geoResolvedActor;
              if (childElement.hasAttribute("geo_uid") || childElement
                  .hasAttribute("geo_country")) {
                geoResolvedActor = toGeoResolvedActor(childElement);
              } else {
                geoResolvedActor = null;
              }
              final Optional<Symbol> actorDBName =
                  optionalSymbolAttribute(childElement, "actor_db_name");
              final Optional<Boolean> isAcronym =
                  optionalBooleanAttribute(childElement, "is_acronym");
              final Optional<Boolean>
                  requiresContext = optionalBooleanAttribute(childElement, "requires_context");

              am = ProperNounActorMention.create(actorPatternID, actorID, actorName,
                  sourceNote, geoResolvedActor, associationScore, patternConfidenceScore,
                  patternMatchScore, editDistanceScore, geoResolutionScore, importanceScore,
                  mention,
                  actorDBName, isAcronym, requiresContext);
            } else { // We have neither paired_agent_uid nor actor_uid
              am = SimpleActorMention.create(actorName, sourceNote, mention);
            }
            record(am, childElement);
            actorMentions.add(am);
          }
        }
      }
      final ActorMentions ret = ActorMentions.create(actorMentions.build());
      record(ret, e);
      return ret;
    }

    private GeoResolvedActor toGeoResolvedActor(final Element e) {
      final Optional<Long> geoID = optionalLongAttribute(e, "geo_uid");
      final Optional<Symbol> geoCountry = optionalSymbolAttribute(e, "geo_country");
      final GeoResolvedActor.CountryInfo countryInfo;
      final Optional<Long> countryID = optionalLongAttribute(e, "country_id");
      if (countryID.isPresent()) {
        final Symbol isoCode = requiredSymbolAttribute(e, "iso_code");
        final Long countryInfoActorID = requiredLongAttribute(e, "country_info_actor_id");
        final Optional<Symbol> databaseKey = optionalSymbolAttribute(e, "actor_db_name");
        final GeoResolvedActor.CountryInfo.CountryInfoActor countryInfoActor =
            GeoResolvedActor.CountryInfo.CountryInfoActor.create(countryInfoActorID,
                databaseKey);
        final Optional<Symbol> countryInfoActorCode = optionalSymbolAttribute(e,
            "country_info_actor_code");
        countryInfo =
            GeoResolvedActor.CountryInfo.create(countryID.get(), isoCode, countryInfoActor,
                countryInfoActorCode);
      } else {
        countryInfo = null;
      }

      //Do not record geoResolvedActor, since it's not present explicitly in SeriXML and therefore lacks an id
      return GeoResolvedActor.create(optionalSymbolAttribute(e, "geo_text").orNull(),
          geoCountry.orNull(), geoID, optionalDoubleAttribute(e, "geo_latitude"),
          optionalDoubleAttribute(
              e, "geo_longitude"), Optional.fromNullable(countryInfo));
    }

    private Mentions toMentionSet(final Element e) {
      final ImmutableList.Builder<Mention> mentions = ImmutableList.builder();
      final Parse parse = fetch("parse_id", e);

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Mention")) {
            final String typeString = requiredAttribute(childElement, "entity_type");
            final EntityType entityType = EntityType.of(Symbol.from(typeString));

            final EntitySubtype entitySubtype = EntitySubtype.of(
                requiredAttribute(childElement, "entity_subtype"));

            final boolean isMetonymy = requiredBooleanAttribute(childElement, "is_metonymy");
            final MetonymyInfo metonymyInfo;
            if (isMetonymy) {
              metonymyInfo = new MetonymyInfo(
                  EntityType.of(requiredAttribute(childElement, "role_type")),
                  EntityType.of(requiredAttribute(childElement, "intended_type")));
            } else {
              metonymyInfo = null;
            }

            final Mention.Type mentionType =
                Mention.typeForSymbol(Symbol.from(requiredAttribute(childElement, "mention_type")));
            final double confidence = optionalDoubleAttribute(childElement, "confidence").or(1.0);
            final double linkConfidence =
                optionalDoubleAttribute(childElement, "link_confidence").or(1.0);

            SynNode synNodeTmp;
            boolean synNodeIsTerminalFromToken = false;
            Token startTokenTmp = null;
            Token endTokenTmp = null;

            if (childElement.hasAttribute("syn_node_id")) {
              synNodeTmp = this.fetch("syn_node_id", childElement);
              if (childElement.hasAttribute("start_token")) {
                startTokenTmp = this.fetch("start_token", childElement);
                endTokenTmp = this.fetch("end_token", childElement);
              }
            } else {
              startTokenTmp = this.fetch("start_token", childElement);
              endTokenTmp = this.fetch("end_token", childElement);
              synNodeTmp = parse.nodeForToken(endTokenTmp); // use terminal parse node for last token in mention as synNode
              synNodeIsTerminalFromToken = true;
            }

            final Token startToken = startTokenTmp;
            final Token endToken = endTokenTmp;
            final SynNode synNode = synNodeTmp;

            final Symbol external_id = getExternalID(childElement).orNull();

            final Mention m;
            if (!synNodeIsTerminalFromToken) {
              m = synNode.setMention(mentionType, entityType, entitySubtype, metonymyInfo,
                      confidence, linkConfidence, external_id);
            } else {
              m = new Mention(startToken, endToken, synNode, mentionType, entityType, entitySubtype,
                      metonymyInfo, confidence, linkConfidence, external_id);
            }
            m.setModel(optionalStringAttribute(childElement,"model"));
            m.setPattern(optionalStringAttribute(childElement,"pattern"));
            record(m, childElement);
            mentions.add(m);
          }
        }
      }

      // second pass for child, parent, next pointers
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Mention")) {
            final Mention m = fetch(requiredAttribute(childElement, "id"));
            final Mention parentMention = fetchFromOptionalAttribute("parent", childElement);
            final Mention childMention = fetchFromOptionalAttribute("child", childElement);
            final Mention nextMention = fetchFromOptionalAttribute("next", childElement);

            if (parentMention != null) {
              m.setParent(parentMention);
            }

            if (childMention != null) {
              m.setChild(childMention);
            }

            if (nextMention != null) {
              m.setNext(nextMention);
            }
          }
        }
      }

      final float descScore = Float.parseFloat(e.getAttribute("desc_score"));
      final float nameScore = Float.parseFloat(e.getAttribute("name_score"));
      final Mentions ms = new Mentions.Builder()
          .mentions(mentions.build())
          .parse(parse)
          .descScore(descScore)
          .nameScore(nameScore)
          .build();
      record(ms, e);
      return ms;
    }

    private Entities toEntitySet(final Element e) {
      final ImmutableList.Builder<Entity> entitiesList = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (is(childElement, "Entity")) {
            final Entity.Builder entityB = Entity.builder()
                .type(EntityType.of(requiredAttribute(childElement, "entity_type")))
                .subtype(EntitySubtype.of(requiredAttribute(childElement, "entity_subtype")))
                .generic(requiredBooleanAttribute(childElement, "is_generic"))
                .guid(optionalIntegerAttribute(childElement, "guid"))
                .externalID(getExternalID(childElement));

            // code below  slightly complicated to eliminate duplicates while preserving
            // order and correspondence with mention confidence list.
            // There ought not to be any duplicates, but there is a bug in (at least)
            // Chinese SERIF which causes them to occur occasionally.
            final List<String> mentionIds =
                Splitter.on(CharMatcher.WHITESPACE).omitEmptyStrings()
                    .trimResults()
                    .splitToList(requiredAttribute(childElement, "mention_ids"));

            final List<MentionConfidence> confidences =
                loadMentionConfidencesForEntity(childElement, mentionIds);

            final Set<String> seenMentionIds = new HashSet<>();
            for (int i=0; i<mentionIds.size(); ++i) {
              final String mentionId = mentionIds.get(i);
              if (!seenMentionIds.contains(mentionId)) {
                seenMentionIds.add(mentionId);
                final Mention mention = this.fetch(mentionId);
                entityB.addMentionSet(mention);
                entityB.putConfidences(mention, confidences.get(i));
              }
            }

            final Entity entity = entityB.build();
            entitiesList.add(entity);
            record(entity, childElement);
          }
        }
      }

      final Entities entities = Entities.create(entitiesList.build(), e.getAttribute("score"));
      record(entities, e);
      return entities;
    }

    private List<MentionConfidence> loadMentionConfidencesForEntity(final Element element,
        final List<String> mentionIds) {
      final Optional<String> confidenceAttribute =
          optionalStringAttribute(element, "mention_confidences");
      final List<MentionConfidence> confidences;
      if (confidenceAttribute.isPresent()) {
        confidences = new ArrayList<>();
        for (final String confidenceString : Splitter.on(CharMatcher.WHITESPACE)
            .omitEmptyStrings()
            .trimResults().split(confidenceAttribute.get())) {
          try {
            confidences.add(MentionConfidence.valueOf(confidenceString));
          } catch (IllegalArgumentException iae) {
            // we used to write these as integers instead of Strings...
            try {
              confidences.add(MentionConfidence.parseOrdinal(
                  Integer.parseInt(confidenceString)));
            } catch (Exception nfe) {
              throw iae;
            }
          }
        }
        if (confidences.size() != mentionIds.size()) {
          throw new RuntimeException("For entity, number of mention confidences does not "
              + "match number of mentions");
        }
      } else {
        confidences = Collections.nCopies(mentionIds.size(), MentionConfidence.UnknownConfidence);
      }
      return confidences;
    }

    private ValueMentions toDocumentLevelValueMentionSet(final Element e,
        final List<TokenSequence> tokenSequences) {
      final ImmutableList.Builder<ValueMention> valueMentions = ImmutableList.builder();
      //System.out.println(e.getAttribute("id"));
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (childElement.getTagName().equals("ValueMention")) {
            valueMentions.add(toDocumentLevelValueMention(childElement, tokenSequences));
          }
        }
      }

      final ValueMentions vms = ValueMentions.create(valueMentions.build());
      record(vms, e);
      return vms;
    }

    private ValueMentions toValueMentionSet(final Element e) {
      final TokenSequence ts = fetch("token_sequence_id", e);
      final ImmutableList.Builder<ValueMention> valueMentions = ImmutableList.builder();

      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (childElement.getTagName().equals("ValueMention")) {
            valueMentions.add(toValueMention(childElement, ts));
          }
        }
      }

      final ValueMentions vms = ValueMentions.create(valueMentions.build());
      record(vms, e);
      return vms;
    }


    private DocumentEventArguments toDocumentEventArguments(final Element element,
        LocatedString originalText) {
      final ImmutableList.Builder<DocumentEvent.Argument> ret = ImmutableList.builder();

      for (final Element e : childrenWithTag(element, DOC_EVENT_ARG_ELEMENT)) {
        ret.add(toDocumentEventArgument(e, originalText));
      }

      return DocumentEventArguments.of(ret.build());
    }

    private DocumentEvent.Argument toDocumentEventArgument(final Element e,
        LocatedString originalText) {
      final DocumentEvent.Argument.Builder retB = DocumentEvent.Argument.builder()
          .type(requiredSymbolAttribute(e, "type"))
          .role(requiredSymbolAttribute(e, "role"));

      final Optional<Double> score = optionalDoubleAttribute(e, "score");
      if (score.isPresent()) {
        retB.score(score.get());
      }

      // fillers
      final Optional<Element> textFillerElement = directChild(e, "TextFiller");
      final Optional<Element> entityFillerElement = directChild(e, "EntityFiller");
      final Optional<Element> valueFillerElement = directChild(e, "ValueFiller");
      final Optional<Element> valueMentionFillerElement = directChild(e, "ValueMentionFiller");

      OptionalUtils.exactlyOnePresentOrIllegalState(ImmutableList.of(
          textFillerElement, entityFillerElement,
          valueFillerElement, valueMentionFillerElement),
          "Must have exactly one of TextFiller or EntityFiller");
      final DocumentEvent.Argument.ArgumentFiller filler;
      if (textFillerElement.isPresent()) {
        final DocumentEvent.TextFiller.Builder fillerBuilder = DocumentEvent.TextFiller.builder()
            .text(toLocatedString(textFillerElement.get(), originalText));
        // the canonical string stuff has to be repeated for each element because we
        // can't make a common interface for the builders :-(
        final Optional<Element> canonicalStringEl =
            directChild(textFillerElement.get(), "CanonicalString");
        if (canonicalStringEl.isPresent()) {
          fillerBuilder
              .canonicalString(optionalStringAttribute(canonicalStringEl.get(), "canonicalString"))
              // should we use all the offsets here? Probably.
              .canonicalStringOffsets(offsetsForElement(canonicalStringEl.get())).build();
        }
        filler = fillerBuilder.build();
      } else if (entityFillerElement.isPresent()) {
        final DocumentEvent.EntityFiller.Builder fillerBuilder =
            DocumentEvent.EntityFiller.builder()
                .entity(this.<Entity>fetch("entity_id", entityFillerElement.get()));
        final Optional<Element> canonicalStringEl =
            directChild(entityFillerElement.get(), "CanonicalString");
        if (canonicalStringEl.isPresent()) {
          fillerBuilder
              .canonicalString(optionalStringAttribute(canonicalStringEl.get(), "canonicalString"))
              .canonicalStringOffsets(offsetsForElement(canonicalStringEl.get()));
        }
        filler = fillerBuilder.build();
      } else if (valueFillerElement.isPresent()) {
        final DocumentEvent.ValueFiller.Builder fillerBuilder = DocumentEvent.ValueFiller.builder()
            .value(this.<Value>fetch("value_id", valueFillerElement.get()));
        final Optional<Element> canonicalStringEl =
            directChild(valueFillerElement.get(), "CanonicalString");
        if (canonicalStringEl.isPresent()) {
          fillerBuilder
              .canonicalString(optionalStringAttribute(canonicalStringEl.get(), "canonicalString"))
              .canonicalStringOffsets(offsetsForElement(canonicalStringEl.get()));
        }
        filler = fillerBuilder.build();
      } else if (valueMentionFillerElement.isPresent()) {
        final DocumentEvent.ValueMentionFiller.Builder fillerBuilder =
            DocumentEvent.ValueMentionFiller.builder()
                .valueMention(
                    this.<ValueMention>fetch("value_mention_id", valueMentionFillerElement.get()));
        final Optional<Element> canonicalStringEl =
            directChild(valueMentionFillerElement.get(), "CanonicalString");
        if (canonicalStringEl.isPresent()) {
          fillerBuilder
              .canonicalString(optionalStringAttribute(canonicalStringEl.get(), "canonicalString"))
              .canonicalStringOffsets(offsetsForElement(canonicalStringEl.get()));
        }
        filler = fillerBuilder.build();
      } else {
        throw new SerifXMLException(
            "Document event argument must have one of TextFiller, EntityFiller,"
                + "ValueFiller, or ValueMentionFiller");
      }

      retB.filler(filler);

      // justifications
      retB.justifications(loadJustifications(e, originalText));
      retB.scoredAttributes(loadScoredAttributes(e));
      retB.externalID(getExternalID(e));

      final Optional<ImmutableMap<Symbol, String>> metadata = loadMetadata(e);
      if (metadata.isPresent()) {
        retB.metadata(metadata.get());
      }

      final Optional<Element> provenancesElement = directChild(e, "Provenances");
      if (provenancesElement.isPresent()) {
        for (final Element provenanceElement : childrenWithTag(provenancesElement.get(),
            "EventMentionProvenance")) {
          retB.addProvenances(DocumentEvent.EventMentionArgumentProvenance.builder().argument(
              this.<EventMention.Argument>fetch("event_mention_arg_id", provenanceElement))
              .eventMention(Optional.fromNullable(
                  this.<EventMention>fetchFromOptionalAttribute("event_mention_id",
                      provenanceElement))).build());
        }
      }

      final DocumentEvent.Argument ret = retB.build();
      record(ret, e);
      return ret;
    }

    private DocumentEvents toDocumentEvents(final Element e, LocatedString originalText) {
      final ImmutableList.Builder<DocumentEvent> ret = ImmutableList.builder();
      for (final Element docEventEl : childrenWithTag(e, DOC_EVENT_ELEMENT)) {
        ret.add(toDocumentEvent(docEventEl, originalText));
      }
      return DocumentEvents.of(ret.build());
    }

    private DocumentEvent toDocumentEvent(Element e, LocatedString originalText) {
      final DocumentEvent.Builder ret = DocumentEvent.builder()
          .primaryType(requiredSymbolAttribute(e, "type"))
          .score(optionalDoubleAttribute(e, "score"));
      ret.externalID(getExternalID(e));

      ret.justifications(loadJustifications(e, originalText));
      ret.scoredAttributes(loadScoredAttributes(e));

      for (final Element argsEl : childrenWithTag(e, "Arguments")) {
        for (final Element argEl : elementChildren(argsEl)) {
          if ("ArgumentReference".equals(argEl.getTagName())) {
            ret.addArguments(this.<DocumentEvent.Argument>fetch("id", argEl));
          } else if (DOC_EVENT_ARG_ELEMENT.equals(argEl.getTagName())) {
            ret.addArguments(toDocumentEventArgument(argEl, originalText));
          } else {
            throw new SerifXMLException("Unknown argument element " + argEl.getTagName());
          }
        }
      }

      for (final Element provenancesEl : childrenWithTag(e, "Provenances")) {
        for (final Element provEl : elementChildren(provenancesEl)) {
          if ("EventMentionProvenance".equals(provEl.getTagName())) {
            ret.addProvenances(DocumentEvent.EventMentionProvenance.of(
                this.<EventMention>fetch("event_mention_id", provEl)));
          } else if ("FlexibleEventMentionProvenance".equals(provEl.getTagName())) {
            ret.addProvenances(DocumentEvent.FlexibleEventMentionProvenance.of(
                this.<FlexibleEventMention>fetch("flexible_event_mention_id", provEl)));
          } else if ("TextualProvenance".equals(provEl.getTagName())) {
            final DocumentEvent.TextualProvenance.Builder textualProvenance =
                DocumentEvent.TextualProvenance.builder();
            for (final Element lsEl : childrenWithTag(provEl, "LocatedString")) {
              textualProvenance.addLocatedStrings(toLocatedString(lsEl, originalText));
            }
            ret.addProvenances(textualProvenance.build());
          } else {
            throw new SerifXMLException("Unknown provenance element " + provEl.getTagName());
          }
        }
      }

      return ret.build();
    }


    private Multimap<Symbol, LocatedString> loadJustifications(final Element e,
        LocatedString originalText) {
      final ImmutableMultimap.Builder<Symbol, LocatedString> ret = ImmutableMultimap.builder();
      for (final Element justificationsElement : childrenWithTag(e, "Justifications")) {
        for (final Element justificationElement : childrenWithTag(justificationsElement,
            "Justification")) {
          ret.put(requiredSymbolAttribute(justificationElement, "type"),
              toLocatedString(justificationElement, originalText));
        }
      }
      return ret.build();
    }

    public ImmutableMap<Symbol, Double> loadScoredAttributes(Element e) {
      final ImmutableMap.Builder<Symbol, Double> ret = ImmutableMap.builder();
      for (final Element scoredAttributesElement : childrenWithTag(e, "ScoredAttributes")) {
        for (final Element scoredAttributeElement : childrenWithTag(scoredAttributesElement,
            "ScoredAttribute")) {
          ret.put(requiredSymbolAttribute(scoredAttributeElement, "name"),
              requiredDoubleAttribute(scoredAttributeElement, "score"));
        }
      }
      return ret.build();
    }

    private Values toValueSet(final Element e) {
      final ImmutableList.Builder<Value> values = ImmutableList.builder();
      for (Node child = e.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof Element) {
          final Element childElement = (Element) child;
          if (childElement.getTagName().equalsIgnoreCase("Value")) {
            final ValueMention vm = fetch("value_mention_ref", childElement);
            final Symbol timexVal = nonEmptySymbolOrNull(childElement, "timex_val");
            final Symbol timexAnchorVal = nonEmptySymbolOrNull(childElement, "timex_anchor_val");
            final Symbol timexAnchorDir = nonEmptySymbolOrNull(childElement, "timex_anchor_dir");
            final Symbol timexSet = nonEmptySymbolOrNull(childElement, "timex_set");
            final Symbol timexMode = nonEmptySymbolOrNull(childElement, "timex_mod");
            final Symbol timexNonSpecific =
                nonEmptySymbolOrNull(childElement, "timex_non_specific");
            vm.setDocValue(timexVal, timexAnchorVal, timexAnchorDir, timexSet, timexMode,
                timexNonSpecific);
            // we know this get() will succeed because we just setDocValue above
            //noinspection OptionalGetWithoutIsPresent
            final Value docValue = vm.documentValue().get();
            record(docValue, childElement);
            values.add(docValue);
          }
        }
      }

      final Values vs = Values.create(values.build());
      record(vs, e);
      return vs;
    }

    private Name toName(final Element e, final TokenSequence ts) {
      checkArgument(e.getTagName().equals("Name"));
      final EntityType entityType = EntityType.of(requiredAttribute(e, "entity_type"));
      final OffsetGroupRange offsets = offsetsForElement(e);
      final Optional<Integer> startTokenIndex =
          ts.tokenIndexStartingAt(offsets.startInclusive().charOffset());
      final Optional<Integer> endTokenIndex =
          ts.tokenIndexEndingAt(offsets.endInclusive().charOffset());
      final Optional<Double> score = optionalDoubleAttribute(e, "score");
      final Optional<String> transliteration = optionalStringAttribute(e, "transliteration");
      final Optional<Symbol> externalID = getExternalID(e);

      if (!startTokenIndex.isPresent()) {
        throw new SerifXMLException(String
            .format("Unable to find a start token for name span %s with offsets %d", e,
                offsets.startInclusive().charOffset().asInt()));
      }
      if (!endTokenIndex.isPresent()) {
        throw new SerifXMLException(
            String.format("Unable to find an end token for name span %s", e));
      }
      final Name nameSpan =
          new Name.Builder().span(ts.span(startTokenIndex.get(), endTokenIndex.get()))
              .type(entityType)
              .score(score)
              .transliteration(transliteration)
              .externalID(externalID).build();
      record(nameSpan, e);
      return nameSpan;
    }

    private NestedName toNestedNameTheory(final Element e, final TokenSequence ts) {
      checkArgument(e.getTagName().equals("NestedName"));
      final EntityType entityType = EntityType.of(requiredAttribute(e, "entity_type"));
      final OffsetGroupRange offsets = offsetsForElement(e);
      final Optional<Integer> startTokenIndex =
          ts.tokenIndexStartingAt(offsets.startInclusive().charOffset());
      final Optional<Integer> endTokenIndex =
          ts.tokenIndexEndingAt(offsets.endInclusive().charOffset());
      final Name parent = fetch("parent", e);
      final Optional<Double> score = optionalDoubleAttribute(e, "score");
      final Optional<String> transliteration = optionalStringAttribute(e, "transliteration");

      if (!startTokenIndex.isPresent()) {
        throw new SerifXMLException(String
            .format("Unable to find a start token for name span %s with offsets %d", e,
                offsets.startInclusive().charOffset().asInt()));
      }
      if (!endTokenIndex.isPresent()) {
        throw new SerifXMLException(
            String.format("Unable to find an end token for name span %s", e));
      }
      final NestedName nameSpan =
          NestedName
              .builder(ts.span(startTokenIndex.get(), endTokenIndex.get()), entityType, parent)
              .withTransliteration(transliteration.orNull()).withScore(score.orNull()).build();
      record(nameSpan, e);
      return nameSpan;
    }

    private Parse toParse(final Element e) {
      checkArgument(is(e, "Parse"));
      final float score = requiredFloatAttribute(e, "score");
      final String parseId = requiredAttribute(e, "id");
      final TokenSequence ts = fetch("token_sequence_id", e);

      // empty sentences have special empty parse
      if (ts.isEmpty()) {
        // we still need to record this somehow for the sentence theory beam to refer to it,
        // so we use a fake object as the key
        final Parse emptyParse = Parse.emptyParse(ts);
        record(emptyParse, e);
        return emptyParse;
      }

      SynNode root = null;

      for (Node childNode = e.getFirstChild(); childNode != null;
           childNode = childNode.getNextSibling()) {
        if (childNode instanceof Element) {
          final Element childElement = (Element) childNode;
          if (childElement.getTagName().equalsIgnoreCase("TreeBankString")) {
            if (!requiredAttribute(childElement, "node_id_method").equalsIgnoreCase("DFS")) {
              throw new NotImplementedException(
                  "Currently Java code only knows how to do DFS node ids");
            }
            root = parseTreebankString(childElement.getTextContent(), parseId, ts);
          }
        }
      }

      // it is okay for root to be null - checkMissing expects a nullable argument
      //noinspection ConstantConditions
      checkMissing(e, root, "syn nodes");
      final Parse parse = Parse.create(ts, root, score);
      record(parse, e);
      return parse;
    }

    private SynNode parseTreebankString(final String tbString, final String parseId,
        final TokenSequence ts) {
      final TreebankStringParser parser = TreebankStringParser.create();
      final SynNode synNode = parser.parseTreebankString(tbString, ts);
      // Assign SynNode IDs via a DFS traversal of the parse tree
      assignIds(synNode, parseId, 0);
      return synNode;
    }

    private int assignIds(final SynNode synNode, final String parseId, int dfsCount) {
      final String synNodeId = parseId + "." + dfsCount;
      record(synNode, synNodeId);
      for (final SynNode childSynNode : synNode) {
        dfsCount = assignIds(childSynNode, parseId, dfsCount + 1);
      }
      return dfsCount;
    }

    private ValueMention toDocumentLevelValueMention(final Element e,
        final List<TokenSequence> tokenSequences) {
      final int sentenceNumber = requiredIntegerAttribute(e, "sent_no");
      return toValueMention(e, tokenSequences.get(sentenceNumber));
    }

    private ValueMention toValueMention(final Element e, final TokenSequence ts) {
      final Token startToken = fetch("start_token", e);
      final Token endToken = fetch("end_token", e);
      final ValueType valueType = ValueType.parseDottedPair(requiredAttribute(e, "value_type"));
      final Optional<Symbol> external_id = getExternalID(e);

      final ValueMention vm = ValueMention.builder(valueType,
          ts.span(startToken.index(), endToken.index())).setExternalID(external_id.orNull())
          .build();
      record(vm, e);
      return vm;
    }

    /**
     * Parses an {@link OffsetGroup} that has an arbitrary attribute name.
     */
    private Optional<OffsetGroup> namedOffsetGroupForElement(final Element e,
        final String attributeName) {
      final Optional<String> allOffsets = optionalStringAttribute(e, attributeName);
      if (!allOffsets.isPresent()) {
        return Optional.absent();
      }
      Optional<CharOffset> charOffset = Optional.absent();
      Optional<EDTOffset> edtOffset = Optional.absent();
      Optional<ByteOffset> byteOffset = Optional.absent();
      for (final String offsetPart : allOffsets.get().split(":")) {
        checkState(offsetPart.length() >= 2, "Cannot parse an offset with unknown type or span!");
        final String type = offsetPart.substring(0, 1);
        final int offsetVal = Integer.parseInt(offsetPart.substring(1));
        switch (type) {
          case "c":
            charOffset =
                Optional.of(CharOffset.asCharOffset(offsetVal));
            break;
          case "e":
            edtOffset =
                Optional.of(EDTOffset.asEDTOffset(offsetVal));
            break;
          case "b":
            byteOffset =
                Optional.of(ByteOffset.asByteOffset(offsetVal));
            break;
          default:
            throw new SerifException("Unknown offset type for " + offsetPart);
        }
      }
      final OffsetGroup.Builder ret = new OffsetGroup.Builder();
      if (charOffset.isPresent()) {
        ret.charOffset(charOffset.get());
      }
      if (edtOffset.isPresent()) {
        ret.edtOffset(edtOffset.get());
      } else {
        ret.edtOffset(EDTOffset.asEDTOffset(charOffset.get().asInt()));
      }
      if (byteOffset.isPresent()) {
        ret.byteOffset(byteOffset.get());
      }
      return Optional.of(ret.build());
    }

    private OffsetGroupRange offsetsForElement(final Element e) {
      Range<Integer> edtOffsets = parseRange(requiredAttribute(e, "edt_offsets"));
      Range<Integer> charOffsets = parseRange(requiredAttribute(e, "char_offsets"));
      Optional<Range<Integer>> byteOffsets = parseRange(
          optionalStringAttribute(e, "byte_offsets"));

      if (sourceDocumentOffsetShift.isPresent()) {
        // see class javadoc for 'compatibility with documents offset into source' for more details
        final OffsetGroup.Builder start = new OffsetGroup.Builder();
        start.charOffset(CharOffset.asCharOffset(charOffsets.lowerEndpoint())
            .shiftedCopy(-1 * sourceDocumentOffsetShift.get().charOffset().asInt()));
        start.edtOffset(EDTOffset.asEDTOffset(edtOffsets.lowerEndpoint())
            .shiftedCopy(-1 * sourceDocumentOffsetShift.get().edtOffset().asInt()));

        final OffsetGroup.Builder end = new OffsetGroup.Builder();
        end.charOffset(CharOffset.asCharOffset(charOffsets.upperEndpoint())
            .shiftedCopy(-1 * sourceDocumentOffsetShift.get().charOffset().asInt()));
        end.edtOffset(EDTOffset.asEDTOffset(edtOffsets.upperEndpoint())
            .shiftedCopy(-1 * sourceDocumentOffsetShift.get().edtOffset().asInt()));

        if (byteOffsets.isPresent()) {
          checkState(sourceDocumentOffsetShift.get().byteOffset().isPresent(),
              "Starting ByteOffsets must be defined if any document offsets are ByteOffsets");
          start.byteOffset(ByteOffset.asByteOffset(byteOffsets.get().lowerEndpoint())
              .shiftedCopy(-1 * sourceDocumentOffsetShift.get().byteOffset().get().asInt()));
          end.byteOffset(ByteOffset.asByteOffset(byteOffsets.get().upperEndpoint())
              .shiftedCopy(-1 * sourceDocumentOffsetShift.get().byteOffset().get().asInt()));
        }

        return OffsetGroupRange.from(start.build(), end.build());
      }

      if (byteOffsets.isPresent()) {
        return OffsetGroupRange.from(
            OffsetGroup.from(
                ByteOffset.asByteOffset(byteOffsets.get().lowerEndpoint()),
                CharOffset.asCharOffset(charOffsets.lowerEndpoint()),
                EDTOffset.asEDTOffset(edtOffsets.lowerEndpoint())),
            OffsetGroup.from(
                ByteOffset.asByteOffset(byteOffsets.get().upperEndpoint()),
                CharOffset.asCharOffset(charOffsets.upperEndpoint()),
                EDTOffset.asEDTOffset(edtOffsets.upperEndpoint())));
      }

      return OffsetGroupRange.from(
          OffsetGroup.from(CharOffset.asCharOffset(charOffsets.lowerEndpoint()),
              EDTOffset.asEDTOffset(edtOffsets.lowerEndpoint())),
          OffsetGroup.from(CharOffset.asCharOffset(charOffsets.upperEndpoint()),
              EDTOffset.asEDTOffset(edtOffsets.upperEndpoint())));
    }

    private Range<Integer> parseRange(final String rangeString) {
      final Matcher m = OFFSET_PATTERN.matcher(rangeString);
      if (m.find()) {
        return Range.closed(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
      } else {
        throw new SerifXMLException(String.format("Cannot parse %s as a range", rangeString));
      }
    }

    private Optional<Range<Integer>> parseRange(final Optional<String> rangeString) {
      if (rangeString.isPresent()) {
        return Optional.of(parseRange(rangeString.get()));
      } else {
        return Optional.absent();
      }
    }

    private void record(final Object o, final Element e) {
      record(o, requiredAttribute(e, "id"));
    }

    private void recordIgnoringDuplication(final Object o, final Element e) {
      record(o, requiredAttribute(e, "id"), true);
    }

    private void record(final Object o, final String id) {
      record(o, id, false);
    }

    private void record(final Object o, final String id, boolean ignore_duplication) {
      if (idMap.containsKey(id) && idMap.get(id) != o && !ignore_duplication) {
        throw new SerifXMLException(String.format("A mapping is already present for id %s", id));
      } else {
        idMap.put(id, o);
      }
    }

    // cast unavoidable for heterogeneous container
    @SuppressWarnings("unchecked")
    private <T> T fetch(final String id) {
      checkNotNull(id);
      checkArgument(!id.isEmpty());
      final T ret = (T) idMap.get(id);
      if (ret == null) {
        throw new SerifXMLException(String.format("Lookup failed for id %s.", id));
      }
      return ret;
    }

    // cast unavoidable for heterogeneous container
    @SuppressWarnings("unchecked")
    private <T> T fetch(final String attribute, final Element e) {
      final String attVal = requiredAttribute(e, attribute);

      final Object o = idMap.get(requiredAttribute(e, attribute));

      if (o == null) {
        throw new SerifXMLException(
            String.format("Lookup failed for id %s. Known keys are %s", attVal, idMap.keySet()));
      }

      try {
        return (T) o;
      } catch (final ClassCastException f) {
        throw new SerifXMLException(
            String.format("Didn't expect ID %s to be %s", attVal, o.getClass()));
      }
    }

    // cannot avoid unchecked casts with heterogeneous collections
    @SuppressWarnings("unchecked")
    private <T> List<T> fetchListOrEmpty(final String attribute, final Element e) {
      final String attVals = e.getAttribute(attribute);
      if (attVals.isEmpty()) {
        return ImmutableList.of();
      }

      final List<String> parts = StringUtils.onCommas().splitToList(attVals);
      final List<T> ret = new ArrayList<>();
      for (final String part : parts) {
        final T resolved = (T) idMap.get(part);
        if (resolved != null) {
          ret.add(resolved);
        } else {
          throw new SerifXMLException(String.format("Lookup failed for id %s.", part));
        }
      }
      return ret;
    }

    // cast unavoidable for heterogeneous container
    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T fetchFromOptionalAttribute(final String attribute, final Element e) {
      final String att = e.getAttribute(attribute);

      if (!att.isEmpty()) {
        final T ret = (T) idMap.get(att);

        if (ret == null) {
          throw new SerifXMLException(
              String.format("Lookup failed for id %s. Known keys are %s", att, idMap.keySet()));
        }

        return ret;
      } else {
        return null;
      }
    }

    // cast unavoidable for heterogeneous container
    @SuppressWarnings("unchecked")
    private <T> T fetchIfKnownFromRequiredAttribute(final String attribute, final Element e) {
      final String att = requiredAttribute(e, attribute);

      return (T) idMap.get(att);
    }

    private Optional<ImmutableMap<Symbol, String>> loadMetadata(Element e) {
      final Optional<Element> metadataEl = directChild(e, "Metadata");
      if (metadataEl.isPresent()) {
        final ImmutableMap.Builder<Symbol, String> ret = ImmutableMap.builder();
        for (final Element metadataEntry : childrenWithTag(metadataEl.get(),
            "MetadataEntry")) {
          ret.put(requiredSymbolAttribute(metadataEntry, "key"),
              requiredAttribute(metadataEntry, "value"));
        }
        return Optional.of(ret.build());
      }
      return Optional.absent();
    }
  }


  public static final class Builder extends ImmutableSerifXMLLoader.Builder {

    /**
     * Synonym for {@link #allowSloppyOffsets(boolean)}. For backwards-compatibility.
     */
    public Builder allowSloppyOffsets() {
      return allowSloppyOffsets(true);
    }

    /**
     * Synonym for {@link #compatibilityWithDocumentsOffsetIntoSource(boolean)} (boolean)}.
     * For backwards-compatibility.
     */
    public Builder compatibilityWithDocumentsOffsetIntoSource() {
      return compatibilityWithDocumentsOffsetIntoSource(true);
    }

    /**
     * @deprecated Type sources are no longer used, so all types are basically dynamic now. See
     * issue #316
     */
    @Deprecated
    public Builder makeAllTypesDynamic() {
      log.warn("Unnecessary call to makeAllTypesDynamic. See text-group/jserif#316");
      return this;
    }
  }

  // deprecated methods

  /**
   * @deprecated Use injection.
   */
  @Deprecated
  @SuppressWarnings({"deprecation", "unused"})
  public static SerifXMLLoader createFrom(final Parameters params) throws IOException {
    return SerifXMLLoader.builder().build();
  }

  /**
   * @deprecated Use injection.
   */
  @SuppressWarnings("deprecation")
  @Deprecated
  public static SerifXMLLoader createFromParamsIndirectly(final Parameters params)
      throws IOException {
    return createFrom(Parameters.loadSerifStyle(params.getExistingFile("serifParams")));
  }

  /**
   * @deprecated Prefer to inject your `SerifXMLLoader`. Failing that, use {@link #builder()}
   * because type sources are no longer used. See #316.
   */
  @Deprecated
  public static Builder builderFromStandardACETypes() throws IOException {
    return new Builder();
  }

  /**
   * @deprecated Prefer to inject your `SerifXMLLoader`. Failing that, use {@link #builder()}
   * because type sources are no longer used. See #316.
   */
  @Deprecated
  public static Builder builderWithDynamicTypes() {
    return new Builder();
  }

  /**
   * @deprecated Prefer to inject your `SerifXMLLoader`. Failing that, use {@link #builder()}
   * because type sources are no longer used. See #316.
   */
  @Deprecated
  public static SerifXMLLoader fromStandardACETypes() throws IOException {
    return builder().build();
  }

  /**
   * @deprecated Prefer to inject your `SerifXMLLoader`. Failing that, use {@link #builder()}
   * because type sources are no longer used. See #316.
   */
  @SuppressWarnings({"deprecation", "unused"})
  @Deprecated
  public static SerifXMLLoader fromStandardACETypes(final boolean allowDynamic) throws IOException {
    return builderFromStandardACETypes().build();
  }
}

