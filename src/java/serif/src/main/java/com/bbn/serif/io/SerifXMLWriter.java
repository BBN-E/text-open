package com.bbn.serif.io;

import com.bbn.bue.common.StringUtils;
import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetGroup;
import com.bbn.bue.common.strings.offsets.OffsetGroupRange;
import com.bbn.bue.common.strings.offsets.OffsetGroupSpan;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.bue.common.symbols.SymbolUtils;
import com.bbn.bue.common.temporal.Timex2Time;
import com.bbn.serif.common.Segment;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.Dependencies;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.DocumentEvent;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.Event;
import com.bbn.serif.theories.Event.Argument;
import com.bbn.serif.theories.EventEventRelationMention;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.EventMentions;
import com.bbn.serif.theories.Gloss;
import com.bbn.serif.theories.HasExternalID;
import com.bbn.serif.theories.HasMetadata;
import com.bbn.serif.theories.LexicalForm;
import com.bbn.serif.theories.LexicalFormFunctions;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Mentions;
import com.bbn.serif.theories.Morph;
import com.bbn.serif.theories.MorphFeatureFunctions;
import com.bbn.serif.theories.MorphToken;
import com.bbn.serif.theories.MorphTokenAnalysis;
import com.bbn.serif.theories.MorphTokenAnalysisFunctions;
import com.bbn.serif.theories.MorphTokenFunctions;
import com.bbn.serif.theories.MorphTokenSequence;
import com.bbn.serif.theories.MorphTokenSequenceFunctions;
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
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SentenceTheoryBeam;
import com.bbn.serif.theories.SentenceTheoryBeamFunctions;
import com.bbn.serif.theories.SentenceTheoryFunctions;
import com.bbn.serif.theories.Spanning;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.TokenSequence.Span;
import com.bbn.serif.theories.TokenSpan;
import com.bbn.serif.theories.Value;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.ValueMentions;
import com.bbn.serif.theories.Zone;
import com.bbn.serif.theories.acronyms.Acronym;
import com.bbn.serif.theories.actors.ActorEntity;
import com.bbn.serif.theories.actors.ActorMention;
import com.bbn.serif.theories.actors.ActorMentions;
import com.bbn.serif.theories.actors.CompositeActorMention;
import com.bbn.serif.theories.actors.GeoResolvedActor;
import com.bbn.serif.theories.actors.ProperNounActorMention;
import com.bbn.serif.theories.facts.Fact;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMention;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMentionArgument;
import com.bbn.serif.theories.icewseventmentions.ICEWSEventMention;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharSink;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static com.bbn.bue.common.StringUtils.joinFunction;
import static com.bbn.bue.common.StringUtils.spaceJoiner;
import static com.google.common.base.Preconditions.checkNotNull;

//import com.bbn.serif.theories.Zone;

public final class SerifXMLWriter {

  protected static final Logger log = LoggerFactory.getLogger(SerifXMLWriter.class);

  @SuppressWarnings("deprecation")
  public static SerifXMLWriter create() {
    return new SerifXMLWriter();
  }

  @Inject
  SerifXMLWriter() {

  }

  /**
   * Prefer {@link #saveTo(DocTheory, CharSink)}.
   */
  @Deprecated
  public void saveTo(final DocTheory docTheory, final String filename) throws IOException {
    saveTo(docTheory, new File(filename));
  }

  public void saveTo(final DocTheory docTheory, final File file) throws IOException {
    saveTo(docTheory, Files.asCharSink(file, Charsets.UTF_8));
  }

  public void saveTo(final DocTheory docTheory,final StringWriter stringWriter){
    try {
      final Document xmldoc = (new PerDocumentWriter()).toXMLDocument(docTheory);

      // write the content into xml file
      final TransformerFactory transformerFactory = TransformerFactory.newInstance();
      final Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//			transformer.setOutputProperty("{http://xml.apache.org/xalan}line-separator" ,"\n");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      final DOMSource source = new DOMSource(xmldoc);
      //if (System.getProperty("line.separator").equals("\n")){
      //*nix, where the line seperator is just the newline
      final StreamResult result = new StreamResult(stringWriter);
      transformer.transform(source, result);

//			}else{
//				//for Windows.
//				//This is necessary because otherwise the result will have windows character encodings.
//				//If the result has windows character encodings, then the
//				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//				StreamResult result = new StreamResult(outputStream);
//				transformer.transform(source, result);
//				String content = outputStream.toString();
//				outputStream.close();
//				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
//				writer.write(content.replace("\r\n", "\n"));
//				writer.close();
//			}

    } catch (final TransformerException | IOException tfe) {
      throw new SerifException("Error transforming XML to file", tfe);
    }

  }

  public void saveTo(final DocTheory docTheory, final CharSink sink) throws IOException {
    try {
      final Document xmldoc = (new PerDocumentWriter()).toXMLDocument(docTheory);

      // write the content into xml file
      final TransformerFactory transformerFactory = TransformerFactory.newInstance();
      final Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//			transformer.setOutputProperty("{http://xml.apache.org/xalan}line-separator" ,"\n");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      final DOMSource source = new DOMSource(xmldoc);
      //if (System.getProperty("line.separator").equals("\n")){
      //*nix, where the line seperator is just the newline
      final StringWriter stringWriter = new StringWriter();
      final StreamResult result = new StreamResult(stringWriter);
      transformer.transform(source, result);
      sink.write(stringWriter.toString());
//			}else{
//				//for Windows.
//				//This is necessary because otherwise the result will have windows character encodings.
//				//If the result has windows character encodings, then the
//				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//				StreamResult result = new StreamResult(outputStream);
//				transformer.transform(source, result);
//				String content = outputStream.toString();
//				outputStream.close();
//				OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file),"UTF-8");
//				writer.write(content.replace("\r\n", "\n"));
//				writer.close();
//			}

    } catch (final TransformerException tfe) {
      throw new SerifException("Error transforming XML to file", tfe);
    }
//		} catch (IOException e) {
//			throw new SerifException("Error transforming XML to file", e);
//		}
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private final class PerDocumentWriter {

    public Document toXMLDocument(final DocTheory docTheory) throws IOException {
      idMapByIdentity.clear();
      idMapByValue.clear();
      nextId = 1;

      try {
        final DocumentBuilderFactory factory = DocumentBuilderFactory
            .newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder;

        builder = factory.newDocumentBuilder();

        final Document xmldoc = builder.newDocument();
        final Element rootElement = xmldoc.createElement(SerifXML.SERIFXML_ELEMENT);
        rootElement.setAttribute(SerifXML.VERSION_ATTRIBUTE, SerifXML.SERIFXML_VERSION);
        buildDocument(xmldoc, rootElement, docTheory);
        xmldoc.appendChild(rootElement);
        return xmldoc;
      } catch (final ParserConfigurationException e) {
        throw new SerifException("XML parser configuration error", e);
      }
    }

    private void buildDocument(final Document xmlDoc,
        final Element parentElement, final DocTheory docTheory) throws IOException {
      final Element document = xmlDoc.createElement(SerifXML.DOCUMENT_ELEMENT);
      final com.bbn.serif.theories.Document serifDoc = docTheory.document();
      final LocatedString originalText = serifDoc.originalText();

      setElementIdByObjectByIdentity(document, serifDoc);
      document.setAttribute(SerifXML.DOCID_ATTRIBUTE, serifDoc.name().toString());
      document.setAttribute(SerifXML.LANGUAGE_ATTRIBUTE, serifDoc.language().language().longName());
      document.setAttribute(SerifXML.SOURCE_TYPE_ATTRIBUTE, serifDoc.sourceType().isPresent() ?
                                                            serifDoc.sourceType().get().toString()
                                                                                              : "UNKNOWN");
      if (serifDoc.offsetIntoSource().isPresent()) {
        setOffsetIntoSourceDocument(document, serifDoc.offsetIntoSource().get());
      }
      document.setAttribute("is_downcased", "FALSE");
      if (serifDoc.jodaDocumentTimeInterval().isPresent()){
        document.setAttribute(SerifXML.DOCUMENT_TIME_START_ATTRIBUTE,
            serifDoc.jodaDocumentTimeInterval().get().getStart().toString("yyyy-MM-dd'T'HH:mm:ss"));
        document.setAttribute(SerifXML.DOCUMENT_TIME_END_ATTRIBUTE,
            serifDoc.jodaDocumentTimeInterval().get().getEnd().toString("yyyy-MM-dd'T'HH:mm:ss"));
      }

      //Original Text
        final Element originalTextElement = xmlDoc.createElement(SerifXML.ORIGINAL_TEXT_ELEMENT);
      final LocatedString otext = serifDoc.originalText();
        setElementIdByObjectByIdentity(originalTextElement, otext);
        setElementLocatedStringAttributes(xmlDoc, originalTextElement, otext, originalText);
        document.appendChild(originalTextElement);

      //Regions
      if (serifDoc.regions().isPresent()) {
        final Element regionsElement = xmlDoc.createElement(SerifXML.REGIONS_ELEMENT);
        for (final Region r : serifDoc.regions().get()) {
          regionsElement.appendChild(regionToXML(xmlDoc, r, originalText));
        }
        document.appendChild(regionsElement);
      }

      //Zones
      if (serifDoc.zoning().isPresent()) {
        final Element zonesElement = xmlDoc.createElement(SerifXML.ZONES_ELEMENT);
        final Element zoningElement = xmlDoc.createElement(SerifXML.ZONING_ELEMENT);

        for (final Zone r : serifDoc.zoning().get().rootZones()) {
          zoningElement.appendChild(zoneToXML(xmlDoc, r, originalText));
        }
        zonesElement.appendChild(zoningElement);

        document.appendChild(zonesElement);
      }

      //Segments
      if (serifDoc.segments().isPresent()) {
        final Element segmentsElement = xmlDoc.createElement(SerifXML.SEGMENTS_ELEMENT);
        for (final Segment s : serifDoc.segments().get()) {
          segmentsElement.appendChild(segmentToXML(xmlDoc, s, originalText));
        }
        document.appendChild(segmentsElement);
      }

      //Metadata
      if (serifDoc.metadata().isPresent()) {
        final Element metadataElement = xmlDoc.createElement(SerifXML.METADATA_ELEMENT);
        for (final OffsetGroupSpan s : serifDoc.metadata().get().spans()) {
          metadataElement.appendChild(metadataToXML(xmlDoc, s));
        }
        document.appendChild(metadataElement);
      }

      // gather all morphology algorithms referenced in the document
      final Optional<Element> algorithmsElement = createAlgorithmsElement(docTheory, xmlDoc);
      if (algorithmsElement.isPresent()) {
        document.appendChild(algorithmsElement.get());
      }

      // since morphological analyses may show a lot of redundancy (e.g. same word used many
      // times in a document), we gather all the morphological analysis in the entire document
      // and then used pointers to them from the MorphTokenSequences
      final Optional<Element> morphologicalAnalyses = createMorphologyElement(docTheory, xmlDoc);
      if (morphologicalAnalyses.isPresent()) {
        document.appendChild(morphologicalAnalyses.get());
      }

      //Sentences
      final Element sentencesElement = xmlDoc.createElement(SerifXML.SENTENCES_ELEMENT);
      for (final SentenceTheoryBeam stb : docTheory.sentenceTheoryBeams()) {
        sentencesElement.appendChild(sentenceTheoryBeamToXml(xmlDoc, stb, originalText));
      }
      document.appendChild(sentencesElement);

      if (!docTheory.entities().isAbsent()) {
        //EntitySet
        final Element entitySetElement = xmlDoc.createElement(SerifXML.ENTITY_SET_ELEMENT);
        setElementIdByObjectByIdentity(entitySetElement, docTheory.entities());
        for (final Entity entity : docTheory.entities()) {
          entitySetElement.appendChild(entitySetToXML(xmlDoc, entity));
        }
        entitySetElement.setAttribute("score", docTheory.entities().score());
        document.appendChild(entitySetElement);
      }

      if (!docTheory.values().isAbsent()) {
        //ValueSet
        final Element valueSetElement = xmlDoc.createElement(SerifXML.VALUE_SET_ELEMENT);
        setElementIdByObjectByIdentity(valueSetElement, docTheory.values());
        //valueSetElement.setAttribute("id", newIdString());
        for (final Value value : docTheory.values()) {
          valueSetElement.appendChild(docValueToXML(xmlDoc, value));
        }
        document.appendChild(valueSetElement);
      }

      if (!docTheory.relations().isAbsent()) {
        //RelationSet
        final Element relationSetElement = xmlDoc.createElement(SerifXML.RELATION_SET_ELEMENT);
        setElementIdByObjectByIdentity(relationSetElement, docTheory.relations());
        for (final Relation relation : docTheory.relations()) {
          relationSetElement.appendChild(docRelationsToXML(xmlDoc, relation));
        }
        document.appendChild(relationSetElement);
      }

      if (!docTheory.events().isAbsent()) {
        //EventSet
        final Element eventSetElement = xmlDoc.createElement(SerifXML.EVENT_SET_ELEMENT);
        setElementIdByObjectByIdentity(eventSetElement, docTheory.events());
        for (final Event event : docTheory.events()) {
          eventSetElement.appendChild(docEventToXML(xmlDoc, event));
        }
        document.appendChild(eventSetElement);
      }

      writeDocLevelEvents(docTheory, document, xmlDoc, originalText);

      if (!docTheory.actorEntities().isAbsent()) {
        //ActorEntitySet
        final Element actorEntitySetElement =
            xmlDoc.createElement(SerifXML.ACTOR_ENTITY_SET_ELEMENT);
        setElementIdByObjectByIdentity(actorEntitySetElement, docTheory.actorEntities());
        for (final ActorEntity actorEntity : docTheory.actorEntities()) {
          actorEntitySetElement.appendChild(actorEntitiesToXML(xmlDoc, actorEntity));
        }
        document.appendChild(actorEntitySetElement);
      }

      if (docTheory.documentActorInfo().isPresent()) {
        //DocumentActorInfo
        final Element documentActorInfoElement = xmlDoc.createElement(
            SerifXML.DOCUMENT_ACTOR_INFO_ELEMENT);
        //setElementIdByObject(documentActorInfoElement, docTheory.documentActorInfo().get());
        final Element defaultCountryActorElement =
            xmlDoc.createElement(SerifXML.DEFAULT_COUNTRY_ACTOR_ELEMENT);
        defaultCountryActorElement.setAttribute("actor_uid",
            Long.toString(docTheory.documentActorInfo().get().defaultCountryActorId()));
        documentActorInfoElement.appendChild(defaultCountryActorElement);
        setElementIdByObjectByIdentity(documentActorInfoElement,
            docTheory.documentActorInfo().get());
        document.appendChild(documentActorInfoElement);
      }

      if (!docTheory.facts().isAbsent()) {
        //FactSet
        final Element factSetElement = xmlDoc.createElement(SerifXML.FACT_SET_ELEMENT);
        setElementIdByObjectByIdentity(factSetElement, docTheory.facts());
        for (final Fact fact : docTheory.facts()) {
          factSetElement.appendChild(factsToXML(xmlDoc, fact));
        }
        document.appendChild(factSetElement);
      }

      if (!docTheory.actorMentions().isAbsent()) {
        //ActorMentionSet
        final Element actorMentionSetElement = xmlDoc.createElement(
            SerifXML.ACTOR_MENTION_SET_ELEMENT);
        setElementIdByObjectByIdentity(actorMentionSetElement, docTheory.actorMentions());
        for (final ActorMention actorMention : docTheory.actorMentions()) {
          String sentenceTheoryID =
              idStringByIdentity(actorMention.mention().sentenceTheory(docTheory));
          actorMentionSetElement.appendChild(
              actorMentionsToXML(xmlDoc, actorMention, sentenceTheoryID));
        }
        document.appendChild(actorMentionSetElement);
      }

      if (!docTheory.icewsEventMentions().isAbsent()) {
        //ICEWSEventMentionSet
        final Element icewsEventMentionSetElement =
            xmlDoc.createElement(SerifXML.ICEWS_EVENT_MENTION_SET_ELEMENT);
        setElementIdByObjectByIdentity(icewsEventMentionSetElement, docTheory.icewsEventMentions());
        for (final ICEWSEventMention mention : docTheory.icewsEventMentions()) {
          icewsEventMentionSetElement.appendChild(icewsEventsToXML(xmlDoc, mention));
        }
        document.appendChild(icewsEventMentionSetElement);
      }

      if (!docTheory.flexibleEventMentions().isAbsent()) {
        //FlexibleEventMentionSet
        final Element flexibleEventMentionSetElement =
            xmlDoc.createElement(SerifXML.FLEXIBLE_EVENT_MENTION_SET_ELEMENT);
        setElementIdByObjectByIdentity(flexibleEventMentionSetElement,
            docTheory.flexibleEventMentions());
        for (final FlexibleEventMention mention : docTheory.flexibleEventMentions()) {
          flexibleEventMentionSetElement.appendChild(flexEmsToXML(xmlDoc, mention));
        }
        document.appendChild(flexibleEventMentionSetElement);
      }

      if (!docTheory.valueMentions().isAbsent()) {
        //ValueMentionSet
        final Element valueMentionSetElement = xmlDoc.createElement(
            SerifXML.VALUE_MENTION_SET_ELEMENT);
        setElementIdByObjectByIdentity(valueMentionSetElement, docTheory.valueMentions());
        //valueMentionSetElement.setAttribute("id", newIdString());
        valueMentionSetElement.setAttribute("score", "0");
        for (final ValueMention valMention : docTheory.valueMentions()) {
          valueMentionSetElement.appendChild(docValueMentionsToXML(xmlDoc, valMention));
        }
        document.appendChild(valueMentionSetElement);
      }

      if (!docTheory.relations().isAbsent()) {
        //RelMentionSet
        final Element relationMentionSetElement =
            xmlDoc.createElement(SerifXML.RELATION_MENTION_SET_ELEMENT);
        document.appendChild(relationMentionSetElement);
        //Since this isn't actually being written out with anything, there
        //isn't any harm in using a garbage object to generate the id
        //noinspection RedundantStringConstructorCall
        setElementIdByObjectByIdentity(relationMentionSetElement, new String());
        relationMentionSetElement.setAttribute("score", "0");
      }

      if (docTheory.acronyms().isPresent()) {
        final Element acronymSetElement =
            xmlDoc.createElement(SerifXML.ACRONYM_SET_ELEMENT);
        setElementIdByObjectByIdentity(acronymSetElement, docTheory.acronyms());
        for (final Acronym acronym : docTheory.acronyms().get()) {
          acronymSetElement.appendChild(acronymsToXML(xmlDoc, acronym));
        }
        document.appendChild(acronymSetElement);
      }

      if (!docTheory.eventEventRelationMentions().isAbsent()) {
        //EventEventRelationMentionSet
        final Element eventEventRelationMentionSetElement =
            xmlDoc.createElement(SerifXML.EVENT_EVENT_RELATION_MENTION_SET_ELEMENT);
        setElementIdByObjectByIdentity(eventEventRelationMentionSetElement,
            docTheory.eventEventRelationMentions());
        for (final EventEventRelationMention eventEventRelationMention :
            docTheory.eventEventRelationMentions())
        {
          eventEventRelationMentionSetElement.appendChild(
              eventEventRelationMentionstoXML(xmlDoc, eventEventRelationMention));
          document.appendChild(eventEventRelationMentionSetElement);
        }
      }


      parentElement.appendChild(document);

    }

    private void setOffsetIntoSourceDocument(final Element document, final OffsetGroup offsetGroup) {
      final StringBuilder offsetAttribute = new StringBuilder();
      offsetAttribute.append("c").append(offsetGroup.charOffset().asInt()).append(":");
      offsetAttribute.append("e").append(offsetGroup.edtOffset().asInt());
      if(offsetGroup.byteOffset().isPresent()) {
        offsetAttribute.append(":");
        offsetAttribute.append("b").append(offsetGroup.byteOffset().get().asInt());
      }
      document.setAttribute(SerifXML.OFFSET_INTO_SOURCE, offsetAttribute.toString());
    }

    private Optional<Element> createMorphologyElement(final DocTheory dt, final Document xmlDoc) {
      final ImmutableSet<MorphTokenAnalysis> tokenAnalyses =
          FluentIterable.from(dt.sentenceTheoryBeams())
              .transformAndConcat(SentenceTheoryBeamFunctions.sentenceTheories())
              .transformAndConcat(SentenceTheoryFunctions.morphTokenSequences())
              .transformAndConcat(MorphTokenSequenceFunctions.morphTokens())
              .transformAndConcat(MorphTokenFunctions.analyses())
              .toSet();

      if (!tokenAnalyses.isEmpty()) {
        final Element ret = xmlDoc.createElement("MorphTokenAnalyses");

        final ImmutableSet<LexicalForm> allLemmasAndRoots = FluentIterable.from(tokenAnalyses)
            .transformAndConcat(MorphTokenAnalysisFunctions.lemmas())
            .append(
                FluentIterable.from(tokenAnalyses)
                    .transformAndConcat(MorphTokenAnalysisFunctions.roots()))
            .toSet();

        // we put all glosses first, since they may be shared by lemmas and roots
        final ImmutableSet<Gloss> allGlosses = FluentIterable.from(allLemmasAndRoots)
            .transformAndConcat(LexicalFormFunctions.glosses())
            .toSet();
        for (final Gloss gloss : allGlosses) {
          final Element glossEl = xmlDoc.createElement("Gloss");
          glossEl.setTextContent(gloss.gloss());
          glossEl.setAttribute("id", idStringByValue(gloss));
          ret.appendChild(glossEl);
        }

        // we put all lemmas and roots next, since they may be shared by analyses
        for (final LexicalForm lexicalForm : allLemmasAndRoots) {
          final Element lexicalFormEl = xmlDoc.createElement("LexicalForm");

          lexicalFormEl.setAttribute("id", idStringByValue(lexicalForm));
          lexicalFormEl.setAttribute("form", lexicalForm.form().asString());
          if (lexicalForm.inLexicon()) {
            lexicalFormEl.setAttribute("in_lexicon", "true");
          }
          if (!lexicalForm.glosses().isEmpty()) {
            lexicalFormEl.setAttribute("glosses", FluentIterable.from(lexicalForm.glosses())
                .transform(idStringByValueFunction()).join(StringUtils.commaJoiner()));
          }
          ret.appendChild(lexicalFormEl);
        }

        // and all morphs
        final ImmutableSet<Morph> allMorphs = FluentIterable.from(tokenAnalyses)
            .transformAndConcat(MorphTokenAnalysisFunctions.morphs())
            .toSet();

        for (final Morph morph : allMorphs) {
          final Element morphEl = xmlDoc.createElement("Morph");

          morphEl.setAttribute("id", idStringByValue(morph));
          morphEl.setAttribute("form", morph.form().asString());
          morphEl.setAttribute("type", morph.morphType().name().asString());
          if (!morph.features().isEmpty()) {
            morphEl.setAttribute("features",
                FluentIterable.from(morph.features())
                    .transform(MorphFeatureFunctions.name())
                    .transform(SymbolUtils.desymbolizeFunction())
                    .join(StringUtils.commaJoiner()));
          }

          /*if (!morph.exponentTokenRelativeOffsets().isEmpty()) {
            morphEl.setAttribute("tokRelOffsets",
                FluentIterable.from(morph.exponentTokenRelativeOffsets())
                .transform(CharOffsetsToColonPair.INSTANCE)
                .join(StringUtils.commaJoiner()));
          }*/

          ret.appendChild(morphEl);
        }

        // finally we store all analyses themselves
        for (final MorphTokenAnalysis analysis : tokenAnalyses) {
          final Element analysisEl = xmlDoc.createElement("MorphTokenAnalysis");

          analysisEl.setAttribute("algorithm_id", idStringByValue(analysis.sourceAlgorithm()));
          if (analysis.score().isPresent()) {
            analysisEl.setAttribute("score", Double.toString(analysis.score().get()));
          }

          if (!analysis.lemmas().isEmpty()) {
            analysisEl.setAttribute("lemmas", FluentIterable.from(analysis.lemmas())
                .transform(idStringByValueFunction()).join(StringUtils.commaJoiner()));
          }

          if (!analysis.roots().isEmpty()) {
            analysisEl.setAttribute("roots", FluentIterable.from(analysis.roots())
                .transform(idStringByValueFunction()).join(StringUtils.commaJoiner()));
          }

          if (!analysis.morphs().isEmpty()) {
            analysisEl.setAttribute("morphs", FluentIterable.from(analysis.morphs())
                .transform(idStringByValueFunction()).join(StringUtils.commaJoiner()));
          }

          if (!analysis.morphFeatures().isEmpty()) {
            analysisEl.setAttribute("features", FluentIterable.from(analysis.morphFeatures())
                .transform(MorphFeatureFunctions.name()).transform(SymbolUtils.desymbolizeFunction())
                .join(StringUtils.commaJoiner()));
          }

          analysisEl.setAttribute("id", idStringByValue(analysis));

          ret.appendChild(analysisEl);
        }
        return Optional.of(ret);
      } else {
        return Optional.absent();
      }
    }

    private Optional<Element> createAlgorithmsElement(final DocTheory dt, final Document xmlDoc) {
      final ImmutableSet<MorphologyAlgorithmDescription> morphologyAlgorithms =
          FluentIterable.from(dt.sentenceTheoryBeams())
              .transformAndConcat(SentenceTheoryBeamFunctions.sentenceTheories())
              .transformAndConcat(SentenceTheoryFunctions.morphTokenSequences())
              .transform(MorphTokenSequenceFunctions.sourceAlgorithm())
              .toSet();

      if (!morphologyAlgorithms.isEmpty()) {
        final Element algorithms = xmlDoc.createElement("Algorithms");
        final Element morphologyAlgorithmsEl = xmlDoc.createElement("MorphologyAlgorithms");
        algorithms.appendChild(morphologyAlgorithmsEl);
        for (final MorphologyAlgorithmDescription morphologyAlgorithm : morphologyAlgorithms) {
          morphologyAlgorithmsEl.appendChild(toMorphologyAlgorithmsElement(morphologyAlgorithm, xmlDoc));
        }
        return Optional.of(algorithms);
      } else {
        return Optional.absent();
      }
    }

    private Element toMorphologyAlgorithmsElement(final MorphologyAlgorithmDescription algorithm,
        final Document xml) {
      final Element ret = xml.createElement("MorphologyAlgorithm");

      final ImmutableMap<String, Boolean> algorithmProperties = ImmutableMap.<String, Boolean>builder()
          .put("morphs", algorithm.providesMorphs())
          .put("features", algorithm.providesFeatures())
          .put("lemmas", algorithm.providesLemmas())
          .put("roots", algorithm.providesRoots())
          .put("sequence_scores", algorithm.providesSequenceScores())
          .put("token_scores", algorithm.providesTokenScores())
          .put("glosses_lemmas", algorithm.glossesLemmas())
          .put("glosses_roots", algorithm.glossesRoots())
          .put("has_lexicon", algorithm.hasLexicon()).build();

      ret.setAttribute("name", algorithm.name().asString());
      ret.setAttribute("id", idStringByValue(algorithm));

      for (final Map.Entry<String, Boolean> e : algorithmProperties.entrySet()) {
        ret.setAttribute(e.getKey(), Boolean.toString(e.getValue()));
      }
      return ret;
    }

    private void writeDocLevelEvents(final DocTheory docTheory, final Element document,
        final Document xmlDoc,
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") LocatedString originalText) {

      if (!docTheory.documentEventArguments().isAbsent()) {
        final Element docEventArgsElement =
            xmlDoc.createElement(SerifXML.DOC_EVENT_ARG_SET_ELEMENT);
        for (final DocumentEvent.Argument arg : docTheory.documentEventArguments()) {
          docEventArgsElement.appendChild(toXML(xmlDoc, arg, originalText));
        }
        document.appendChild(docEventArgsElement);
      }

      if (!docTheory.documentEvents().isAbsent()) {
        final Element docEventsElement = xmlDoc.createElement(SerifXML.DOC_EVENT_SET_ELEMENT);
        for (final DocumentEvent docEvent : docTheory.documentEvents()) {
          docEventsElement.appendChild(toXML(xmlDoc, docEvent, originalText));
        }
        document.appendChild(docEventsElement);
      }

    }

    private Node toXML(final Document xmlDoc, final DocumentEvent docEvent,
        final LocatedString originalText) {
      final Element ret = xmlDoc.createElement(SerifXML.DOC_EVENT_ELEMENT);

      setElementIdByObjectByIdentity(ret, docEvent);

      ret.setAttribute("type", docEvent.primaryType().asString());

      // Arguments
      final Element argsElement = xmlDoc.createElement("Arguments");
      for (final DocumentEvent.Argument arg : docEvent.arguments()) {
        final Element argElement;
        if (idMapByIdentity.containsKey(arg)) {
          // this argument previous appeared in another event or in the DocumentEventArguments
          argElement = xmlDoc.createElement("ArgumentReference");
          argElement.setAttribute("id", idStringByIdentity(arg));
        } else {
          argElement = toXML(xmlDoc, arg, originalText);
        }
        argsElement.appendChild(argElement);
      }
      ret.appendChild(argsElement);

      if (!docEvent.provenances().isEmpty()) {
        final Element provenancesElement = xmlDoc.createElement("Provenances");
        for (final DocumentEvent.Provenance provenance : docEvent.provenances()) {
          final Element provEl;
          if (provenance instanceof DocumentEvent.EventMentionProvenance) {
            provEl = xmlDoc.createElement("EventMentionProvenance");
            provEl.setAttribute("event_mention_id",
                idStringByIdentity(
                    ((DocumentEvent.EventMentionProvenance) provenance).eventMention()));
          } else if (provenance instanceof DocumentEvent.FlexibleEventMentionProvenance) {
            provEl = xmlDoc.createElement("FlexibleEventMentionProvenance");
            provEl.setAttribute("flexible_event_mention_id",
                idStringByIdentity(((DocumentEvent.FlexibleEventMentionProvenance) provenance)
                    .flexibleEventMention()));
          } else if (provenance instanceof DocumentEvent.TextualProvenance) {
            provEl = xmlDoc.createElement("TextualProvenance");
            for (final LocatedString locatedString : ((DocumentEvent.TextualProvenance) provenance)
                .locatedStrings()) {
              final Element locatedStringElement = xmlDoc.createElement("LocatedString");
              setElementLocatedStringAttributes(xmlDoc, locatedStringElement,
                  locatedString, originalText);
              provEl.appendChild(locatedStringElement);
            }
          } else {
            throw new SerifException("Unknown provenance type " + provenance.getClass());
          }
          provenancesElement.appendChild(provEl);
        }
        ret.appendChild(provenancesElement);
      }

      writeScoredAttributes(xmlDoc, ret, docEvent.scoredAttributes());
      writeJustifications(xmlDoc, ret, docEvent.justifications(), originalText);
      writeExternalID(docEvent, ret);

      if (docEvent.score().isPresent()) {
        ret.setAttribute("score", Double.toString(docEvent.score().get()));
      }

      return ret;
    }

    private Element toXML(final Document xmlDoc, final DocumentEvent.Argument arg,
        LocatedString originalText) {
      final Element ret = xmlDoc.createElement(SerifXML.DOC_EVENT_ARG_ELEMENT);

      setElementIdByObjectByIdentity(ret, arg);

      ret.setAttribute("type", arg.type().asString());
      ret.setAttribute("role", arg.role().asString());
      writeExternalID(arg, ret);

      // filler
      final Element fillerElement;
      if (arg.filler() instanceof DocumentEvent.TextFiller) {
        fillerElement = xmlDoc.createElement("TextFiller");
        setElementLocatedStringAttributes(xmlDoc, fillerElement,
            ((DocumentEvent.TextFiller) arg.filler()).text(), originalText);
      } else if (arg.filler() instanceof DocumentEvent.EntityFiller) {
        fillerElement = xmlDoc.createElement("EntityFiller");
        fillerElement.setAttribute("entity_id",
            idStringByIdentity(((DocumentEvent.EntityFiller) arg.filler()).entity()));
      } else if (arg.filler() instanceof DocumentEvent.ValueFiller) {
        fillerElement = xmlDoc.createElement("ValueFiller");
        fillerElement.setAttribute("value_id",
            idStringByIdentity(((DocumentEvent.ValueFiller) arg.filler()).value()));
      } else if (arg.filler() instanceof DocumentEvent.ValueMentionFiller) {
        fillerElement = xmlDoc.createElement("ValueMentionFiller");
        fillerElement.setAttribute("value_mention_id",
            idStringByIdentity(((DocumentEvent.ValueMentionFiller) arg.filler()).valueMention()));
      } else {
        throw new SerifException("Unknown doc-level event argument filler type " + arg.filler().getClass());
      }
      if (arg.filler().canonicalString().isPresent()) {
        final Element canonicalEl = xmlDoc.createElement("CanonicalString");
        canonicalEl.setAttribute("canonicalString", arg.filler().canonicalString().get());
        addOffsets(canonicalEl, arg.filler().canonicalStringOffsets().get());
        fillerElement.appendChild(canonicalEl);
      }
      ret.appendChild(fillerElement);

      // justifications
      final ImmutableMultimap<Symbol, LocatedString> justifications = arg.justifications();
      writeJustifications(xmlDoc, ret, justifications, originalText);

      // scored attributes & metadata
      writeScoredAttributes(xmlDoc, ret, arg.scoredAttributes());
      writeMetadata(xmlDoc, ret, arg);

      // provenances
      if (!arg.provenances().isEmpty()) {
        final Element provenancesElement = xmlDoc.createElement("Provenances");
        for (final DocumentEvent.Argument.ArgumentProvenance provenance : arg.provenances()) {
          if (provenance instanceof DocumentEvent.EventMentionArgumentProvenance) {
            final Element provenanceElement = xmlDoc.createElement("EventMentionProvenance");
            final DocumentEvent.EventMentionArgumentProvenance eventMentProvenance =
                (DocumentEvent.EventMentionArgumentProvenance) provenance;
            if (eventMentProvenance.eventMention().isPresent()) {
              provenanceElement.setAttribute("event_mention_id",
                  idStringByIdentity(eventMentProvenance.eventMention().get()));
            }
            provenanceElement.setAttribute("event_mention_arg_id",
                idStringByIdentity(eventMentProvenance.argument()));
            provenancesElement.appendChild(provenanceElement);
          } else {
            throw new SerifException("Unknown doc event arg provenance " + provenance.getClass());
          }
        }
        ret.appendChild(provenancesElement);
      }

      if (arg.score().isPresent()) {
        ret.setAttribute("score", Double.toString(arg.score().get()));
      }

      return ret;
    }

    private void writeJustifications(final Document xmlDoc, final Element element,
        final ImmutableMultimap<Symbol, LocatedString> justifications,
        final LocatedString originalText) {
      if (!justifications.isEmpty()) {
        final Element justificationsElement = xmlDoc.createElement("Justifications");
        for (final Map.Entry<Symbol, LocatedString> e : justifications.entries()) {
          final Element justificationElement = xmlDoc.createElement("Justification");
          justificationElement.setAttribute("type", e.getKey().asString());
          setElementLocatedStringAttributes(xmlDoc, justificationElement, e.getValue(),
              originalText);
          justificationsElement.appendChild(justificationElement);
        }
        element.appendChild(justificationsElement);
      }
    }

    private void writeScoredAttributes(final Document xmlDoc, final Element element,
        final ImmutableMap<Symbol, Double> scoredAttributes) {
      if (!scoredAttributes.isEmpty()) {
        final Element attributesElement = xmlDoc.createElement("ScoredAttributes");
        for (final Map.Entry<Symbol, Double> e : scoredAttributes.entrySet()) {
          final Element attributeElement = xmlDoc.createElement("ScoredAttribute");
          attributeElement.setAttribute("name", e.getKey().asString());
          attributeElement.setAttribute("score", Double.toString(e.getValue()));
          attributesElement.appendChild(attributeElement);
        }
        element.appendChild(attributesElement);
      }
    }

    private void writeMetadata(final Document xmlDoc, final Element element,
        final HasMetadata obj) {
      if (!obj.metadata().isEmpty()) {
        final Element attributesElement = xmlDoc.createElement("Metadata");
        for (final Map.Entry<Symbol, String> e : obj.metadata().entrySet()) {
          final Element attributeElement = xmlDoc.createElement("MetadataEntry");
          attributeElement.setAttribute("key", e.getKey().asString());
          attributeElement.setAttribute("value", e.getValue());
          attributesElement.appendChild(attributeElement);
        }
        element.appendChild(attributesElement);
      }
    }

    private Element docEventToXML(final Document xmlDoc, final Event event) {
      final Element ret = xmlDoc.createElement(SerifXML.EVENT_ELEMENT);
      setElementIdByObjectByIdentity(ret, event);
      ret.setAttribute("event_mention_ids", objectsToIdsByIdentity(event.eventMentions()));
      ret.setAttribute("event_type", event.type().toString());
      ret.setAttribute("genericity", event.genericity().toString());
      ret.setAttribute("modality", event.modality().toString());
      ret.setAttribute("polarity", event.polarity().toString());
      ret.setAttribute("tense", event.tense().toString());
      writeExternalID(event, ret);
      for (final Argument arg : event.arguments()) {
        final Element eArg = xmlDoc.createElement(SerifXML.EVENT_ARG_ELEMENT);
        if (arg instanceof Event.EntityArgument) {
          eArg.setAttribute("entity_id", idStringByIdentity(((Event.EntityArgument) arg).entity()));
        } else if (arg instanceof Event.ValueArgument) {
          eArg.setAttribute("value_id", idStringByIdentity(((Event.ValueArgument) arg).value()));
        }
        eArg.setAttribute("role", arg.role().toString());
        ret.appendChild(eArg);
      }
      return ret;
    }

    private void writeExternalID(final HasExternalID obj, final Element e) {
      if (obj.externalID().isPresent()) {
        e.setAttribute(SerifXML.EXTERNAL_ID, obj.externalID().get().asString());
      }
    }

    private Element actorEntitiesToXML(final Document xmlDoc, final ActorEntity actorEntity) {
      final Element ret = xmlDoc.createElement(SerifXML.ACTOR_ENTITY_ELEMENT);
      setElementIdByObjectByIdentity(ret, actorEntity);
      List<ActorMention> actorMentions = actorEntity.actorMentions();
      if (!actorMentions.isEmpty()) {
        ret.setAttribute("actor_mention_ids", objectsToIdsByIdentity(actorMentions));
      }
      //TODO: may consider comparing the returned values with default values
      //optional attributes can be added only if the returned values are not default values
      if (actorEntity.actorID().isPresent()) {
        ret.setAttribute("actor_uid", Long.toString(actorEntity.actorID().get()));
      }
      ret.setAttribute("confidence", Double.toString(actorEntity.confidence()));
      ret.setAttribute("entity_id", idStringByIdentity(actorEntity.entity()));
      Symbol sourceNote = actorEntity.sourceNote().orNull();
      if (sourceNote != null) {
        ret.setAttribute("source_note", sourceNote.toString());
      }
      ret.setAttribute("actor_name",actorEntity.actorName().toString());
      if (actorEntity.actorDBName().isPresent()){
        ret.setAttribute("actor_db_name", actorEntity.actorDBName().get().asString());
      }
      return ret;
    }

    private Element eventEventRelationMentionstoXML(
        final Document xmlDoc, final EventEventRelationMention eventEventRelationMention)
    {
      final Element ret = xmlDoc.createElement(SerifXML.EVENT_EVENT_RELATION_MENTION_ELEMENT);
      setElementIdByObjectByIdentity(ret, eventEventRelationMention);
      ret.setAttribute( "relation_type",
          eventEventRelationMention.relationType().toString());
      if (eventEventRelationMention.confidence().isPresent())
        ret.setAttribute("confidence",
            eventEventRelationMention.confidence().get().toString());
      if (eventEventRelationMention.pattern().isPresent())
        ret.setAttribute("pattern", eventEventRelationMention.pattern().get());
      if (eventEventRelationMention.model().isPresent())
        ret.setAttribute("model", eventEventRelationMention.model().get());
      if (eventEventRelationMention.polarity().isPresent()){
        ret.setAttribute("polarity",eventEventRelationMention.polarity().get().toString());
      }
      if(eventEventRelationMention.triggerText().isPresent()){
          ret.setAttribute("trigger_text",eventEventRelationMention.triggerText().get());
      }
      ret.appendChild(toXML(xmlDoc, eventEventRelationMention.leftEventMention()));
      ret.appendChild(toXML(xmlDoc, eventEventRelationMention.rightEventMention()));

      return ret;
    }

    private Element toXML(final Document xmlDoc, final EventEventRelationMention.Argument arg) {
      final Element eArg;
      if (arg instanceof EventEventRelationMention.EventMentionArgument) {
        EventEventRelationMention.EventMentionArgument ema =
            (EventEventRelationMention.EventMentionArgument) arg;
        eArg = xmlDoc.createElement(SerifXML.EVENT_MENTION_RELATION_ARGUMENT);
        eArg.setAttribute("event_mention_id", idStringByIdentity(ema.eventMention()));
      } else if (arg instanceof EventEventRelationMention.ICEWSEventMentionArgument) {
        EventEventRelationMention.ICEWSEventMentionArgument iema =
            (EventEventRelationMention.ICEWSEventMentionArgument) arg;
        eArg = xmlDoc.createElement(SerifXML.ICEWS_EVENT_MENTION_RELATION_ARGUMENT);
        eArg.setAttribute("icews_event_mention_id", idStringByIdentity(iema.icewsEventMention()));
      } else {
        throw new SerifException("Argument is of an unhandled class: "
            + arg.getClass().getCanonicalName());
      }
      eArg.setAttribute("role", arg.role().toString());
      return eArg;
    }

    private Element factsToXML(final Document xmlDoc, final Fact fact) {
      final Element ret = xmlDoc.createElement(SerifXML.FACT_ELEMENT);
      setElementIdByObjectByIdentity(ret, fact);
      ret.setAttribute("start_sentence", Integer.toString(fact.tokenSpan().startSentenceIndex()));
      ret.setAttribute("end_sentence", Integer.toString(fact.tokenSpan().endSentenceIndex()));
      ret.setAttribute("start_token",
          Integer.toString(fact.tokenSpan().startTokenIndexInclusive()));
      ret.setAttribute("end_token", Integer.toString(fact.tokenSpan().endTokenIndexInclusive()));
      ret.setAttribute("fact_type", fact.type().toString());
      ret.setAttribute("score", Double.toString(fact.score()));
      if (fact.scoreGroup().isPresent()) {
        ret.setAttribute("score_group", Integer.toString(fact.scoreGroup().get()));
      }
      List<Fact.Argument> arguments = fact.arguments();
      if (!arguments.isEmpty()) {
        for (final Fact.Argument arg : arguments) {
          ret.appendChild(toXML(xmlDoc, arg));
        }
      }
      return ret;
    }

    private Element toXML(final Document xmlDoc, final Fact.Argument arg) {
      final Element eArg;
      if (arg instanceof Fact.MentionArgument) {
        eArg = xmlDoc.createElement(SerifXML.MENTION_FACT_ARG_ELEMENT);
        eArg.setAttribute("mention_id", idStringByIdentity(((Fact.MentionArgument) arg).mention()));
      } else if (arg instanceof Fact.ValueMentionArgument) {
        eArg = xmlDoc.createElement(SerifXML.VALUE_MENTION_FACT_ARG_ELEMENT);
        ValueMention valueMention = ((Fact.ValueMentionArgument) arg).valueMention().orNull();
        if (valueMention != null) {
          eArg.setAttribute("value_mention_id", idStringByIdentity(valueMention));
        }
        eArg.setAttribute("is_doc_date",
            Boolean.toString(((Fact.ValueMentionArgument) arg).isDocDate()).toUpperCase());
      } else if (arg instanceof Fact.TextSpanArgument) {
        eArg = xmlDoc.createElement(SerifXML.TEXT_SPAN_FACT_ARG_ELEMENT);
        final TokenSpan argSpan = ((Fact.TextSpanArgument) arg).tokenSpan();
        eArg.setAttribute("start_sentence", Integer.toString(argSpan.startSentenceIndex()));
        eArg.setAttribute("end_sentence", Integer.toString(argSpan.endSentenceIndex()));
        eArg.setAttribute("start_token", Integer.toString(argSpan.startTokenIndexInclusive()));
        eArg.setAttribute("end_token", Integer.toString(argSpan.endTokenIndexInclusive()));
      } else if (arg instanceof Fact.StringArgument) {
        eArg = xmlDoc.createElement(SerifXML.STRING_FACT_ARG_ELEMENT);
        eArg.setAttribute("string", ((Fact.StringArgument) arg).string().toString());
      } else {
        throw new SerifException("Argument is of an unhandled class: "
            + arg.getClass().getCanonicalName());
      }
      eArg.setAttribute("role", arg.role().toString());
      return eArg;
    }

    private Element icewsEventsToXML(final Document xmlDoc, final ICEWSEventMention icewsEventMention) {
      final Element ret = xmlDoc.createElement(SerifXML.ICEWS_EVENT_MENTION_ELEMENT);
      setElementIdByObjectByIdentity(ret, icewsEventMention);
      ret.setAttribute("event_code", icewsEventMention.code().toString());
      ret.setAttribute("event_tense", icewsEventMention.tense().toString());
      ret.setAttribute("is_reciprocal", capsBoolean(icewsEventMention.isReciprocal()));
      setOptionalEmptySymbolAttribute(ret, "original_event_id", icewsEventMention.originalEventId());
      ret.setAttribute("pattern_id", icewsEventMention.patternId().toString());
      setOptionalIdAttributeByIdentity(ret, "time_value_mention_id", icewsEventMention.timeValueMention());
      List<Proposition> propositions = icewsEventMention.propositions();
      ret.setAttribute("proposition_ids", objectsToIdsByIdentity(propositions));

      List<ICEWSEventMention.ICEWSEventParticipant> participants =
          icewsEventMention.eventParticipants();
      for (final ICEWSEventMention.ICEWSEventParticipant participant : participants) {
        final Element partElem = xmlDoc.createElement(SerifXML.ICEWS_EVENT_PARTICIPANT_ELEMENT);
        partElem.setAttribute("actor_id",
            idStringByIdentity(participant.actorMention()));
        partElem.setAttribute("role", participant.role().toString());
        ret.appendChild(partElem);
      }
      return ret;
    }

    //private final Symbol ID = Symbol.from("id");
    private Element flexEmsToXML(final Document xmlDoc, final FlexibleEventMention flexibleEventMention)
        throws IOException {
      final Element ret = xmlDoc.createElement(SerifXML.FLEXIBLE_EVENT_MENTION_ELEMENT);
      setElementIdByObjectByIdentity(ret, flexibleEventMention);
      ret.setAttribute(SerifXML.EVENT_TYPE_ATTRIBUTE, flexibleEventMention.type().asString());
      for (Map.Entry<Symbol, Symbol> entry : flexibleEventMention.attributes().entrySet()) {
        if (entry.getKey().equalTo(Symbol.from(SerifXML.GENERIC_ID_ATTRIBUTE))) {
          // "id" from setElementIdByObject() has a special meaning in saving/loading XML, so we should not overwrite it
          throw new IOException("Cannot write a FlexibleEventMentionArgument with attribute 'id'");
        } else if (!entry.getKey().asString().equals(SerifXML.EXTERNAL_ID)) {
          ret.setAttribute(entry.getKey().asString(), entry.getValue().asString());
        }
      }
      final ImmutableList<FlexibleEventMentionArgument> arguments = flexibleEventMention.arguments();
      for (final FlexibleEventMentionArgument arg : arguments) {
        final Element argXml = toXML(xmlDoc, arg);
        ret.appendChild(argXml);
      }
      writeExternalID(flexibleEventMention, ret);
      return ret;
    }

    private Element toXML(final Document xmlDoc, final FlexibleEventMentionArgument arg) {
      final Element argXml = xmlDoc.createElement(SerifXML.FLEXIBLE_EVENT_MENTION_ARGUMENT_ELEMENT);
      argXml.setAttribute(SerifXML.GENERIC_ROLE_ATTRIBUTE, arg.role().toString());
      writeExternalID(arg, argXml);
      if (arg.mention().isPresent()) {
        argXml.setAttribute(SerifXML.GENERIC_MENTION_ID_ATTRIBUTE,
            idStringByIdentity(arg.mention().get()));
      } else if (arg.valueMention().isPresent()) {
        argXml.setAttribute(SerifXML.GENERIC_VALUE_MENTION_ID_ATTRIBUTE,
            idStringByIdentity(arg.valueMention().get()));
      } else if (arg.synNode().isPresent()) {
        argXml.setAttribute(SerifXML.GENERIC_SYN_NODE_ID_ATTRIBUTE,
            idStringByIdentity(arg.synNode().get()));
      } else{
        final TokenSpan argSpan = arg.tokenSpan();
        argXml.setAttribute(SerifXML.GENERIC_START_SENTENCE_ATTRIBUTE,
            Integer.toString(argSpan.startSentenceIndex()));
        argXml.setAttribute(SerifXML.GENERIC_END_SENTENCE_ATTRIBUTE,
            Integer.toString(argSpan.endSentenceIndex()));
        argXml.setAttribute(SerifXML.GENERIC_START_TOKEN_ATTRIBUTE,
            Integer.toString(argSpan.startTokenIndexInclusive()));
        argXml.setAttribute(SerifXML.GENERIC_END_TOKEN_ATTRIBUTE,
            Integer.toString(argSpan.endTokenIndexInclusive()));
      }
      if(arg.geographicalResolution().isPresent()){
        final GeoResolvedActor geoResolvedActor = arg.geographicalResolution().get();
        if(geoResolvedActor.geoID().isPresent()){
          argXml.setAttribute(SerifXML.GENERIC_GEO_ID_ATTRIBUTE,
              Long.toString(geoResolvedActor.geoID().get()));
        }else if(geoResolvedActor.geoCountry().isPresent()){
          argXml.setAttribute(SerifXML.GENERIC_GEO_COUNTRY_ATTRIBUTE,
              geoResolvedActor.geoCountry().get().toString());
        }
      }else if(arg.temporalResolution().isPresent()){
        final Element timex2Element = toXML(xmlDoc,arg.temporalResolution().get());
        argXml.appendChild(timex2Element);
      }
      return argXml;
    }

    private Element toXML(final Document xmlDoc, final Timex2Time timex2Time){
      final Element timex2Elem = xmlDoc.createElement(SerifXML.TIMEX2_ELEMENT);
      if(timex2Time.value().isPresent()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_VAL_ATTRIBUTE,timex2Time.value().get().asString());
      }
      if(timex2Time.modifier().isPresent()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_MOD_ATTRIBUTE, timex2Time.modifier().get().name());
      }
      if(timex2Time.isSet()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_SET_ATTRIBUTE, capsBoolean(timex2Time.isSet()));
      }
      if(timex2Time.granularity().isPresent()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_GRANULARITY_ATTRIBUTE,
            timex2Time.granularity().get().asString());
      }
      if(timex2Time.periodicity().isPresent()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_PERIODICITY_ATTRIBUTE,
            timex2Time.periodicity().get().asString());
      }
      if(timex2Time.anchorValue().isPresent()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_ANCHOR_VAL_ATTRIBUTE,
            timex2Time.anchorValue().get().asString());
      }
      if(timex2Time.anchorDirection().isPresent()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_ANCHOR_DIR_ATTRIBUTE,
            timex2Time.anchorDirection().get().name());
      }
      if(timex2Time.isNonSpecific()){
        timex2Elem.setAttribute(SerifXML.TIMEX2_NON_SPECIFIC_ATTRIBUTE,
            capsBoolean(timex2Time.isNonSpecific()));
      }
      return timex2Elem;
    }

    private Element docRelationsToXML(final Document xmlDoc, final Relation relation) {
      final Element ret = xmlDoc.createElement(SerifXML.RELATION_ELEMENT);
      setElementIdByObjectByIdentity(ret, relation);
      ret.setAttribute("confidence", relation.confidence().toString());
      ret.setAttribute("left_entity_id", idStringByIdentity(relation.leftEntity()));
      ret.setAttribute("right_entity_id", idStringByIdentity(relation.rightEntity()));
      ret.setAttribute("rel_mention_ids", objectsToIdsByIdentity(relation.relationMentions()));
      ret.setAttribute("modality", relation.modality().toString());
      ret.setAttribute("tense", relation.tense().toString());
      ret.setAttribute("type", relation.type().toString());
      writeExternalID(relation, ret);
      return ret;
    }

    private Element docValueToXML(final Document xmlDoc, final Value value) {
      final Element ret = xmlDoc.createElement(SerifXML.VALUE_ELEMENT);
      setElementIdByObjectByIdentity(ret, value);
      setOptionalEmptySymbolAttribute(ret, "timex_val", value.timexAnchorDir());
      setOptionalEmptySymbolAttribute(ret, "timex_anchor_val", value.timexAnchorVal());
      setOptionalEmptySymbolAttribute(ret, "timex_mod", value.timexMod());
      setOptionalEmptySymbolAttribute(ret, "timex_non_specific", value.timexNonSpecific());
      setOptionalEmptySymbolAttribute(ret, "timex_set", value.timexSet());
      setOptionalEmptySymbolAttribute(ret, "timex_val", value.timexVal());
      ret.setAttribute("type", value.fullType().name().toString());
      ret.setAttribute("value_mention_ref", idStringByIdentity(value.valueMention()));
      return ret;
    }

    private Element entitySetToXML(final Document xmlDoc, final Entity entity) {
      final Element ret = xmlDoc.createElement(SerifXML.ENTITY_ELEMENT);
      setElementIdByObjectByIdentity(ret, entity);
      ret.setAttribute("entity_type", entity.type().toString());
      if (entity.subtype().isUndetermined()) {
        ret.setAttribute("entity_subtype", "UNDET");
      } else {
        ret.setAttribute("entity_subtype", entity.subtype().name().toString());
      }
      writeExternalID(entity, ret);
      ret.setAttribute("is_generic", capsBoolean(entity.generic()));
      ret.setAttribute("mention_ids", objectsToIdsByIdentity(entity.mentionSet()));

      final List<String> confidenceStrings = Lists.newArrayList();
      for (Mention mention : entity.mentionSet()) {
        confidenceStrings.add(entity.confidence(mention).toString());
      }
      ret.setAttribute("mention_confidences", Joiner.on(" ").join(confidenceStrings));

      return ret;
    }

    private Element regionToXML(final Document xmlDoc, final Region region,
        final LocatedString originalText) {
      final Element ret = xmlDoc.createElement(SerifXML.REGION_ELEMENT);
      setElementIdByObjectByIdentity(ret, region);
      setElementLocatedStringAttributes(xmlDoc, ret, region.content(), originalText);
      ret.setAttribute("is_receiver", capsBoolean(region.isReceiverRegion()));
      ret.setAttribute("is_speaker", capsBoolean(region.isSpeakerRegion()));

      // for backwards compatability with CSerif we write missing region tags as empty strings
      ret.setAttribute("tag", region.tag().or(Symbol.from("")).asString());
      return ret;
    }

    private Element acronymsToXML(final Document xmlDoc, final Acronym acronym) {
      final Element ret = xmlDoc.createElement(SerifXML.ACRONYM_ELEMENT);
      setElementIdByObjectByIdentity(ret, acronym);
      ret.appendChild(toXML(xmlDoc, acronym.getAcronym(), "acronym"));
      if (acronym.getExpansion().isPresent()) {
        ret.appendChild(toXML(xmlDoc, acronym.getExpansion().get(), "expansion"));
      }
      return ret;
    }

    private Element toXML(final Document xmlDoc, final Acronym.Provenance provenance, final String type) {
      final Element ret = xmlDoc.createElement(SerifXML.PROVENANCE_ELEMENT);
      setElementIdByObjectByIdentity(ret, provenance);
      ret.setAttribute("type", type);
      if (provenance.getMention().isPresent()) {
        ret.setAttribute("mention_id", idStringByIdentity(provenance.getMention().get()));
      }
      if (provenance.getTokenSpan().isPresent()) {
        TokenSpan tokenSpan = provenance.getTokenSpan().get();
        ret.setAttribute("start_sentence", Integer.toString(tokenSpan.startSentenceIndex()));
        ret.setAttribute("end_sentence", Integer.toString(tokenSpan.endSentenceIndex()));
        ret.setAttribute("start_token",
            Integer.toString(tokenSpan.startTokenIndexInclusive()));
        ret.setAttribute("end_token", Integer.toString(tokenSpan.endTokenIndexInclusive()));
      }
      ret.setAttribute("text", provenance.getText().toString());

      return ret;
    }

    private Element zoneToXML(final Document xmlDoc, final Zone zone,
        final LocatedString originalText) {
      final Element ret = xmlDoc.createElement(SerifXML.ZONE_ELEMENT);
      setElementIdByObjectByIdentity(ret, zone);

      ret.setAttribute("type", zone.tag().toString());

      for (final Symbol key : zone.attributeSet()) {
        final Element attributeElement = xmlDoc.createElement(SerifXML.LOCATED_ZONE_ATTRIBUTE);
        setElementLocatedStringAttributes(xmlDoc, attributeElement, zone.attribute(key).get(), originalText);
        attributeElement.setAttribute("name", key.toString());
        ret.appendChild(attributeElement);
      }

      if (zone.author().isPresent()) {
        final Element authorElement = xmlDoc.createElement(SerifXML.ZONE_AUTHOR);
        setElementLocatedStringAttributes(xmlDoc, authorElement, zone.author().get(), originalText);
        ret.appendChild(authorElement);
      }

      if (zone.dateTime().isPresent()) {
        final Element dateTimeElement = xmlDoc.createElement(SerifXML.ZONE_DATE_TIME);
        setElementLocatedStringAttributes(xmlDoc, dateTimeElement, zone.dateTime().get(), originalText);
        ret.appendChild(dateTimeElement);
      }

      for (Zone childZone : zone.children()) {
        ret.appendChild(zoneToXML(xmlDoc, childZone, originalText));
      }

      setElementLocatedStringAttributes(xmlDoc, ret, zone.locatedString(), originalText);

      return ret;
    }

    private Element segmentToXML(final Document xmlDoc, final Segment segment,
        LocatedString originalText) {
      final Element ret = xmlDoc.createElement(SerifXML.SEGMENT_ELEMENT);
      log.trace("Unimplemented: Couldn't save segment element.");
      return ret;
    }

    private Element metadataToXML(final Document xmlDoc, final OffsetGroupSpan span) {
      final Element ret = xmlDoc.createElement(SerifXML.SPAN_ELEMENT);
      setElementIdByObjectByIdentity(ret, span);
      addOffsets(ret, span.range());

      ret.setAttribute("span_type", span.type().toString());

      for (final Map.Entry<String, String> otherAttribute : span.attributes().entrySet()) {
        ret.setAttribute(otherAttribute.getKey(), otherAttribute.getValue());
      }
      return ret;
    }

    private Element sentenceTheoryBeamToXml(final Document xmlDoc, final SentenceTheoryBeam sentenceTheoryBeam,
        final LocatedString originalText) {
      final Element ret = xmlDoc.createElement(SerifXML.SENTENCE_ELEMENT);

      ret.setAttribute("id", idStringByIdentity(sentenceTheoryBeam));
      setElementLocatedStringAttributes(xmlDoc, ret,
          sentenceTheoryBeam.sentence().locatedString(), originalText);
      ret.setAttribute("is_annotated", "TRUE"); //TODO: load this properly
      ret.setAttribute("region_id", idStringByIdentity(sentenceTheoryBeam.sentence().region()));

      final Set<Object> writtenParts = new HashSet<>();
      String parseId = null;

      // Reserve id for SentenceTheory to match C++
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        idStringByIdentity(sentenceTheory);
      }

      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.tokenSequence().isAbsent()
            && !writtenParts.contains(sentenceTheory.tokenSequence())) {
          ret.appendChild(tokenSequenceToXML(xmlDoc, sentenceTheory.tokenSequence()));
          writtenParts.add(sentenceTheory.tokenSequence());
        }
      }

      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.morphTokenSequences().isEmpty()) {
          for (final MorphTokenSequence seq : sentenceTheory.morphTokenSequences()) {
            if (!writtenParts.contains(seq)) {
              ret.appendChild(morphTokenSequenceToXML(xmlDoc, seq));
              writtenParts.add(seq);
            }
          }
        }
      }

      Element bogusPOS = null;
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (sentenceTheory.hasPOSSequence()) {
          bogusPOS = bogusPOSSequence(xmlDoc, sentenceTheory.tokenSequence());
          ret.appendChild(bogusPOS);
          break;
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.names().isAbsent()
            && !writtenParts.contains(sentenceTheory.names())) {
          ret.appendChild(namesToXML(xmlDoc, sentenceTheory.names(), sentenceTheory.tokenSequence()));
          writtenParts.add(sentenceTheory.names());
        }
      }

      for(final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (sentenceTheory.nestedNames().isPresent() && !writtenParts
            .contains(sentenceTheory.nestedNames().get())) {
          ret.appendChild((nestedNamesToXML(xmlDoc, sentenceTheory.nestedNames().get(), sentenceTheory.tokenSequence())));
          writtenParts.add(sentenceTheory.nestedNames().get());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.valueMentions().isAbsent()
            && !writtenParts.contains(sentenceTheory.valueMentions())) {
          ret.appendChild(
              valueMentionsToXML(xmlDoc, sentenceTheory.valueMentions(), sentenceTheory.tokenSequence()));
          writtenParts.add(sentenceTheory.valueMentions());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.parse().isAbsent()
            && !writtenParts.contains(sentenceTheory.parse())) {
          ret.appendChild(parseToXML(xmlDoc, sentenceTheory.parse(), sentenceTheory.tokenSequence()));
          writtenParts.add(sentenceTheory.parse());
          parseId = idStringByIdentity(sentenceTheory.parse());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.mentions().isAbsent()
            && !writtenParts.contains(sentenceTheory.mentions())) {
          checkNotNull(parseId, "Parse should have been written before mentions");
          ret.appendChild(mentionsToXML(xmlDoc, sentenceTheory.mentions(), parseId));
          writtenParts.add(sentenceTheory.mentions());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.propositions().isAbsent()
            && !writtenParts.contains(sentenceTheory.propositions())) {
          ret.appendChild(propositionsToXML(xmlDoc, sentenceTheory.propositions(), sentenceTheory.mentions()));
          writtenParts.add(sentenceTheory.propositions());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (!sentenceTheory.dependencies().isAbsent()
            && !writtenParts.contains(sentenceTheory.dependencies())) {
          ret.appendChild(dependenciesToXML(xmlDoc, sentenceTheory.dependencies(), sentenceTheory.mentions()));
          writtenParts.add(sentenceTheory.dependencies());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (sentenceTheory.relationMentions() != null && !sentenceTheory.relationMentions()
            .isAbsent()
            && !writtenParts.contains(sentenceTheory.relationMentions())) {
          ret.appendChild(relationMentionsToXML(xmlDoc, sentenceTheory.relationMentions()));
          writtenParts.add(sentenceTheory.relationMentions());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (sentenceTheory.eventMentions() != null && !sentenceTheory.eventMentions().isAbsent()
            && !writtenParts.contains(sentenceTheory.eventMentions())) {
          ret.appendChild(
              eventMentionsToXML(xmlDoc, sentenceTheory.eventMentions(), parseId));
          writtenParts.add(sentenceTheory.eventMentions());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        if (sentenceTheory.actorMentions() != null && !sentenceTheory.actorMentions().isAbsent()
            && !writtenParts.contains(sentenceTheory.actorMentions())) {
          ret.appendChild(
              actorMentionsToXML(xmlDoc, sentenceTheory.actorMentions(), idStringByIdentity(sentenceTheory)));
          writtenParts.add(sentenceTheory.actorMentions());
        }
      }
      for (final SentenceTheory sentenceTheory : sentenceTheoryBeam) {
        ret.appendChild(theoryToXML(xmlDoc, sentenceTheory, bogusPOS));
      }

      return ret;
    }

    private Element morphTokenSequenceToXML(final Document xmlDoc, final MorphTokenSequence seq) {
      final Element ret = xmlDoc.createElement("MorphTokenSequence");

      ret.setAttribute("id", idStringByValue(seq));
      ret.setAttribute("algorithm_id", idStringByValue(seq.sourceAlgorithm()));
      ret.setAttribute("tok_seq_id", idStringByIdentity(seq.tokenSequence()));
      if (seq.score().isPresent()) {
        ret.setAttribute("score", Double.toString(seq.score().get()));
      }

      for (final MorphToken morphToken : seq.morphTokens()) {
        final Element morphTokEl = xmlDoc.createElement("MorphToken");
        morphTokEl.setAttribute("span",
            morphToken.span().startIndex() + ":" + morphToken.span().endIndex());

        morphTokEl.setAttribute("analyses", FluentIterable.from(morphToken.analyses())
            .transform(idStringByValueFunction())
            .join(StringUtils.commaJoiner()));
        ret.appendChild(morphTokEl);
      }

      return ret;
    }

    // we need to pass in bogusPOS because we don't have a real
    // part-of-speech sequence yet, so its id is mapped to the XML
    // element itself
    private Element theoryToXML(final Document xmlDoc,
        final SentenceTheory st, final Element bogusPOS) {
      final Element ret = xmlDoc.createElement(SerifXML.SENTENCE_THEORY_ELEMENT);

      setElementIdByObjectByIdentity(ret, st);

      if (!st.morphTokenSequences().isEmpty()) {
        ret.setAttribute("morph_tok_seq_ids",
            StringUtils.commaJoiner().join(
                Iterables.transform(st.morphTokenSequences(), idStringByValueFunction())));
      }

      if (st.eventMentions() != null && !st.eventMentions().isAbsent()) {
        ret.setAttribute("event_mention_set_id", idStringByIdentity(st.eventMentions()));
      }
      if (st.relationMentions() != null && !st.relationMentions().isAbsent()) {
        ret.setAttribute("rel_mention_set_id", idStringByIdentity(st.relationMentions()));
      }

      if (!st.mentions().isAbsent()) {
        ret.setAttribute("mention_set_id", idStringByIdentity(st.mentions()));
      }
      if (!st.names().isAbsent()) {
        ret.setAttribute("name_theory_id", idStringByIdentity(st.names()));
      }
      if (st.nestedNames().isPresent()) {
        ret.setAttribute("nested_name_theory_id", idStringByIdentity(st.nestedNames().get()));
      }
      if (!st.parse().isAbsent()) {
        ret.setAttribute("parse_id", idStringByIdentity(st.parse()));
      }

      if (st.hasPOSSequence()) {
        ret.setAttribute("part_of_speech_sequence_id", idStringByIdentity(bogusPOS));
      }
      if (!st.propositions().isAbsent()) {
        ret.setAttribute("proposition_set_id", idStringByIdentity(st.propositions()));
      }
      if (!st.dependencies().isAbsent()) {
        ret.setAttribute("dependency_set_id", idStringByIdentity(st.dependencies()));
      }
      if (!st.tokenSequence().isAbsent()) {
        ret.setAttribute("token_sequence_id", idStringByIdentity(st.tokenSequence()));
      }
      if (!st.valueMentions().isAbsent()) {
        ret.setAttribute("value_mention_set_id", idStringByIdentity(st.valueMentions()));
      }
      if (!st.actorMentions().isAbsent()) {
        ret.setAttribute("actor_mention_set_id", idStringByIdentity(st.actorMentions()));
      }
      ret.setAttribute("primary_parse", "full_parse");

      return ret;
    }

    private Element tokenSequenceToXML(final Document xmlDoc, final TokenSequence ts) {
      final Element ret = xmlDoc.createElement(SerifXML.TOKEN_SEQUENCE_ELEMENT);
      ret.setAttribute("score", scoreString(ts.score()));
      setElementIdByObjectByIdentity(ret, ts);

      for (final Token tok : ts) {
        ret.appendChild(tokenToXML(xmlDoc, tok));
      }

      return ret;
    }

    private Element tokenToXML(final Document xmlDoc, final Token tok) {
      final Element ret = xmlDoc.createElement(SerifXML.TOKEN_ELEMENT);

      setElementIdByObjectByIdentity(ret, tok);
      ret.setTextContent(tok.tokenizedText().utf16CodeUnits());
      ret.setAttribute(SerifXML.CHAR_OFFSETS_ATTRIBUTE, String.format("%d:%d",
          tok.startCharOffset().asInt(), tok.endCharOffset().asInt()));
      ret.setAttribute(SerifXML.EDT_OFFSETS_ATTRIBUTE, String.format("%d:%d",
          tok.startEDTOffset().asInt(), tok.endEDTOffset().asInt()));
      if (tok.startByteOffset().isPresent() && tok.endByteOffset().isPresent()) {
        ret.setAttribute(SerifXML.BYTE_OFFSETS_ATTRIBUTE, String.format("%d:%d",
            tok.startByteOffset().get().asInt(), tok.endByteOffset().get().asInt()));
      }
      return ret;
    }

    private Element bogusPOSSequence(final Document xmlDoc, final TokenSequence ts) {
      // JSerif doesn't currently support POSSequences
      // so we always write an empty one
      final Element ret = xmlDoc.createElement(SerifXML.POS_SEQUENCE_ELEMENT);

      // since we have no POSSequence, we use the element itself
      // as the id key
      setElementIdByObjectByIdentity(ret, ret);
      ret.setAttribute("score", "0");
      ret.setAttribute("token_sequence_id", idStringByIdentity(ts));

      return ret;
    }

    private Element namesToXML(final Document xmlDoc, final Names names,
        final TokenSequence ts) {
      final Element ret = xmlDoc.createElement(SerifXML.NAMES_ELEMENT);

      setElementIdByObjectByIdentity(ret, names);
      if(names.score().isPresent()) {
        ret.setAttribute("score", names.score().get().toString());
      }

      ret.setAttribute("token_sequence_id", idStringByIdentity(ts));

      for (final Name name : names) {
        ret.appendChild(toXML(xmlDoc, name));
      }

      return ret;
    }

    private Element toXML(final Document xmlDoc, final Name name) {
      final Element ret = xmlDoc.createElement(SerifXML.NAME_ELEMENT);

      setElementIdByObjectByIdentity(ret, name);
      if(name.score().isPresent()) {
        ret.setAttribute("score", name.score().get().toString());
      }
      writeExternalID(name, ret);
      ret.setAttribute("entity_type", name.type().name().toString());
      addTokensAndOffsets(ret, name);

      return ret;
    }

    private Element nestedNamesToXML(final Document xmlDoc, final NestedNames nestedNames,
        final TokenSequence ts) {
      final Element ret = xmlDoc.createElement(SerifXML.NESTED_NAMES_ELEMENT);

      setElementIdByObjectByIdentity(ret, nestedNames);
      if(nestedNames.score().isPresent()) {
        ret.setAttribute("score", nestedNames.score().get().toString());
      }
      ret.setAttribute("token_sequence_id", idStringByIdentity(ts));
      ret.setAttribute("name_theory_id", idStringByIdentity(nestedNames.parent()));

      for (final NestedName name : nestedNames) {
        ret.appendChild(toXML(xmlDoc, name));
      }

      return ret;
    }

    private Element toXML(final Document xmlDoc, final NestedName name) {
      final Element ret = xmlDoc.createElement(SerifXML.NESTED_NAME_ELEMENT);

      setElementIdByObjectByIdentity(ret, name);
      if(name.score().isPresent()) {
        ret.setAttribute("score", name.score().get().toString());
      }
      ret.setAttribute("entity_type", name.type().name().toString());
      ret.setAttribute("parent", idStringByIdentity(name.parent()));
      addTokensAndOffsets(ret, name);

      return ret;
    }


    private Element valueMentionsToXML(final Document xmlDoc, final ValueMentions valueMentions,
        final TokenSequence ts) {
      final Element ret = xmlDoc.createElement(SerifXML.VALUE_MENTION_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, valueMentions);
      ret.setAttribute("score", "0");
      ret.setAttribute("token_sequence_id", idStringByIdentity(ts));

      for (final ValueMention vm : valueMentions) {
        ret.appendChild(docValueMentionsToXML(xmlDoc, vm));
      }

      return ret;
    }

    private Element docValueMentionsToXML(final Document xmlDoc, final ValueMention vm) {
      final Element ret = xmlDoc.createElement(SerifXML.VALUE_MENTION_ELEMENT);

      setElementIdByObjectByIdentity(ret, vm);
      ret.setAttribute("value_type", vm.fullType().name().toString());
      ret.setAttribute("sent_no", String.valueOf(vm.span().sentenceIndex()));
      writeExternalID(vm, ret);
      addTokensAndOffsets(ret, vm);

      return ret;
    }

    private Element parseToXML(final Document xmlDoc, final Parse parse,
        final TokenSequence ts) {
      final Element ret = xmlDoc.createElement(SerifXML.PARSE_ELEMENT);

      setElementIdByObjectByIdentity(ret, parse);

      ret.setAttribute("score", Float.toString(parse.score()));
      ret.setAttribute("token_sequence_id", idStringByIdentity(ts));
      ret.appendChild(treebankStringElement(xmlDoc, parse));

      return ret;
    }

    private Element treebankStringElement(final Document xmlDoc, final Parse p) {
      final Element ret = xmlDoc.createElement(SerifXML.TREEBANK_STRING_ELEMENT);

      ret.setAttribute("node_id_method", "DFS");
      final String baseId = idStringByIdentity(p);

      if (p.root().isPresent()) {
        assignDFSIds(p.root().get(), baseId);
        ret.setTextContent(p.root().get().toHeadMarkedFlatString());
      } else {
        // this is the "empty parse" assigned to sentences with no tokens
        ret.setTextContent("(X^ -empty-)");
      }

      return ret;
    }

    // SerifXML represents parse nodes by appending their index in a
    // depth-first walk of the parse tree to the id for the parse
    private void assignDFSIds(final SynNode root, final String baseId) {
      final int[] nextNodeId = new int[1];
      nextNodeId[0] = 0;

      assignDFSIds(root, baseId, nextNodeId);
    }

    // uses an array as a quick way of simulating pass-by-reference
    private void assignDFSIds(final SynNode root, final String baseId,
        final int[] nextNodeId) {
      assignIdByIdentity(root, String.format("%s.%d", baseId, nextNodeId[0]++));
      for (final SynNode node : root) {
        assignDFSIds(node, baseId, nextNodeId);
      }
    }

    private Element mentionsToXML(final Document xmlDoc, final Mentions mentions,
        final String parse_id) {
      final Element ret = xmlDoc.createElement(SerifXML.MENTION_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, mentions);
      ret.setAttribute("desc_score", scoreString(mentions.descScore()));
      ret.setAttribute("name_score", scoreString(mentions.nameScore()));
      ret.setAttribute("parse_id", parse_id);

      //Fetch ids for each mention first in order so that they match the input.
      for (final Mention m : mentions) {
        idStringByIdentity(m);
      }
      for (final Mention m : mentions) {
        ret.appendChild(toXML(xmlDoc, m));
      }

      return ret;
    }

    private String scoreString(final float score) {
      return (new BigDecimal(score, new MathContext(6, RoundingMode.DOWN))).stripTrailingZeros()
          .toPlainString();
    }

    private Element toXML(final Document xmlDoc, final Mention mention) {
      final Element ret = xmlDoc.createElement(SerifXML.MENTION_ELEMENT);

      setElementIdByObjectByIdentity(ret, mention);
      if (mention.entitySubtype().isUndetermined()) {
        ret.setAttribute("entity_subtype", "UNDET");
      } else {
        ret.setAttribute("entity_subtype", mention.entitySubtype().name().toString());
      }
      ret.setAttribute("entity_type", mention.entityType().name().toString());
      ret.setAttribute("mention_type", mention.mentionType().name().toLowerCase());
      ret.setAttribute("syn_node_id", idStringByIdentity(mention.node()));

      ret.setAttribute("confidence", Double.toString(mention.confidence()));
      ret.setAttribute("link_confidence", Double.toString(mention.linkConfidence()));

      setOptionalIdAttributeByIdentity(ret, "child", mention.child());
      setOptionalIdAttributeByIdentity(ret, "parent", mention.parent());
      setOptionalIdAttributeByIdentity(ret, "next", mention.next());
      //link confidence

      ret.setAttribute("is_metonymy", capsBoolean(mention.isMetonymyMention()));
      if (mention.isMetonymyMention()) {
        ret.setAttribute("intended_type",
            mention.metonymyInfo().get().intendedType().name().toString());
        ret.setAttribute("role_type", mention.metonymyInfo().get().role().name().toString());
      } else {
        ret.setAttribute("intended_type", "UNDET");
        ret.setAttribute("role_type", "UNDET");
      }
      writeExternalID(mention, ret);
      if(mention.model().isPresent()){
        ret.setAttribute("model",mention.model().get());
      }
      if(mention.pattern().isPresent()){
        ret.setAttribute("pattern",mention.pattern().get());
      }
      return ret;
    }

    private Element propositionsToXML(final Document xmlDoc, final Propositions props,
        final Mentions ms) {
      final Element ret = xmlDoc.createElement(SerifXML.PROPOSITION_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, props);
      ret.setAttribute("mention_set_id", idStringByIdentity(ms));

      for (final Proposition prop : props) {
        ret.appendChild(toXML(xmlDoc, prop));
      }

      return ret;
    }

    private Element dependenciesToXML(final Document xmlDoc, final Dependencies deps,
        final Mentions ms) {
      final Element ret = xmlDoc.createElement(SerifXML.DEPENDENCY_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, deps);
      ret.setAttribute("mention_set_id", idStringByIdentity(ms));

      for (final Proposition prop : deps) {
        ret.appendChild(toXML(xmlDoc, prop));
      }

      return ret;
    }

    private Element toXML(final Document xmlDoc, final Proposition prop) {
      final Element ret = xmlDoc.createElement(SerifXML.PROPOSITION_ELEMENT);

      setElementIdByObjectByIdentity(ret, prop);
      ret.setAttribute("type", prop.predType().name().toString());
                /*if (prop.predHead().isPresent()) {
                        if (!idMap.containsKey(prop.predHead().get())) {
				System.err.println(prop.predHead().get().toHeadMarkedFlatString());
			}
		}*/
      setOptionalIdAttributeByIdentity(ret, "head_id", prop.predHead());
      setOptionalIdAttributeByIdentity(ret, "modal_id", prop.modal());
      setOptionalIdAttributeByIdentity(ret, "negation_id", prop.negation());
      setOptionalIdAttributeByIdentity(ret, "adverb_id", prop.adverb());
      setOptionalIdAttributeByIdentity(ret, "particle_id", prop.particle());
      if (prop.statuses().size() > 0) {
        ret.setAttribute("status", joinFunction(spaceJoiner()).apply(prop.statuses()));
      }

      for (final Proposition.Argument arg : prop.args()) {
        final Element argElement = xmlDoc.createElement(SerifXML.ARGUMENT_ELEMENT);

        if (arg.role().isPresent()) {
          argElement.setAttribute("role", arg.role().get().toString());
        }

        if (arg instanceof Proposition.MentionArgument) {
          final Proposition.MentionArgument mentArg =
              (Proposition.MentionArgument) arg;
          argElement.setAttribute("mention_id", idStringByIdentity(mentArg.mention()));
        } else if (arg instanceof Proposition.PropositionArgument) {
          final Proposition.PropositionArgument propArg =
              (Proposition.PropositionArgument) arg;
          argElement.setAttribute("proposition_id", idStringByIdentity(propArg.proposition()));
        } else if (arg instanceof Proposition.TextArgument) {
          final Proposition.TextArgument textArg =
              (Proposition.TextArgument) arg;
          argElement.setAttribute("syn_node_id", idStringByIdentity(textArg.node()));
        } else {
          throw new SerifException(String.format("Unknown arg type: %s", arg));
        }

        ret.appendChild(argElement);
      }

      return ret;
    }

    private Element relationMentionsToXML(final Document xmlDoc, final RelationMentions relMentions) {
      final Element ret = xmlDoc.createElement(SerifXML.RELATION_MENTION_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, relMentions);
      ret.setAttribute("score", "0");

      for (final RelationMention rm : relMentions) {
        ret.appendChild(toXML(xmlDoc, rm));
      }

      return ret;
    }

    private Element eventMentionsToXML(final Document xmlDoc, final EventMentions eventMentions,
        final String parseId) {
      final Element ret = xmlDoc.createElement(SerifXML.EVENT_MENTION_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, eventMentions);
      ret.setAttribute("score", "0");
      ret.setAttribute("parse_id", parseId);

      for (final EventMention em : eventMentions) {
        ret.appendChild(toXML(xmlDoc, em));
      }

      return ret;
    }

    private Element toXML(final Document xmlDoc, final EventMention em) {
      final Element ret = xmlDoc.createElement(SerifXML.EVENT_MENTION_ELEMENT);

      setElementIdByObjectByIdentity(ret, em);
      ret.setAttribute("score", Double.toString(em.score()));
      ret.setAttribute("anchor_node_id", idStringByIdentity(em.anchorNode()));
      if (em.anchorProposition().isPresent()) {
        ret.setAttribute("anchor_prop_id", idStringByIdentity(em.anchorProposition().get()));
      }
      writeExternalID(em, ret);
      ret.setAttribute("event_type", em.type().toString());
      ret.setAttribute("genericity", em.genericity().name().toString());
      ret.setAttribute("genericityScore", Double.toString(em.genericityScore()));
      ret.setAttribute("modality", em.modality().name().toString());
      ret.setAttribute("modalityScore", Double.toString(em.modalityScore()));
      ret.setAttribute("polarity", em.polarity().name().toString());
      ret.setAttribute("tense", em.tense().name().toString());
      ret.setAttribute("direction_of_change", em.directionOfChange().name().toString());
      if(em.pattern().isPresent())
        ret.setAttribute("pattern_id", em.pattern().get().toString());
      if(em.semanticPhraseStart().isPresent()) {
        ret.setAttribute("semantic_phrase_start", em.semanticPhraseStart().get().toString());
      }
      if(em.semanticPhraseEnd().isPresent()) {
        ret.setAttribute("semantic_phrase_end", em.semanticPhraseEnd().get().toString());
      }
      if(em.model().isPresent()) {
        ret.setAttribute("model", em.model().get().toString());
      }

      for (final EventMention.Argument arg : em.arguments()) {
        final Element argXml = toXML(xmlDoc, arg, SerifXML.EVENT_MENTION_ARGUMENT_ELEMENT);
        ret.appendChild(argXml);
      }

      for(final EventMention.EventType node : em.eventTypes()) {
        final Element nodeXml = toXML(xmlDoc, node, SerifXML.EVENT_MENTION_TYPE_ELEMENT);
        ret.appendChild(nodeXml);
      }

      for(final EventMention.EventType node : em.factorTypes()) {
        final Element nodeXml = toXML(xmlDoc, node, SerifXML.EVENT_MENTION_FACTOR_TYPE_ELEMENT);
        ret.appendChild(nodeXml);
      }

      for(final EventMention.Anchor node : em.anchors()) {
        final Element nodeXml = toXML(xmlDoc, node, SerifXML.EVENT_MENTION_ANCHOR_ELEMENT);
        ret.appendChild(nodeXml);
      }

      return ret;
    }

    private Element toXML(final Document xmlDoc, final EventMention.EventType node, final String elementName) {
      final Element argXml = xmlDoc.createElement(elementName);
      argXml.setAttribute("event_type", node.eventType().toString());
      argXml.setAttribute("score", Double.toString(node.score()));
      if (node.getMagnitude().isPresent())
        argXml.setAttribute("magnitude", Double.toString(node.getMagnitude().get()));
      if (node.getTrend().isPresent()){
        argXml.setAttribute("trend",node.getTrend().get().name().toString());
      }
      return argXml;
    }

    private Element toXML(final Document xmlDoc, final EventMention.Anchor node, final String elementName) {
      final Element argXml = xmlDoc.createElement(elementName);

      argXml.setAttribute("anchor_node_id", idStringByIdentity(node.anchorNode()));
      if (node.anchorProposition().isPresent()) {
        argXml.setAttribute("anchor_prop_id", idStringByIdentity(node.anchorProposition().get()));
      }
      return argXml;
    }

    private Element toXML(final Document xmlDoc, final EventMention.Argument arg, final String elementName) {
      final Element argXml = xmlDoc.createElement(elementName);
      argXml.setAttribute("role", arg.role().toString());
      argXml.setAttribute("score", Float.toString(arg.score()));
      if (arg instanceof EventMention.MentionArgument) {
        final EventMention.MentionArgument marg = (EventMention.MentionArgument) arg;
        argXml.setAttribute("mention_id", idStringByIdentity(marg.mention()));
      } else if (arg instanceof EventMention.ValueMentionArgument) {
        final EventMention.ValueMentionArgument vmarg = (EventMention.ValueMentionArgument) arg;
        argXml.setAttribute("value_mention_id", idStringByIdentity(vmarg.valueMention()));
      } else if (arg instanceof EventMention.EventMentionArgument) {
        final EventMention.EventMentionArgument emarg = (EventMention.EventMentionArgument) arg;
        argXml.setAttribute("event_mention_id", idStringByIdentity(emarg.eventMention()));
      } else if (arg instanceof EventMention.SpanArgument) {
        final EventMention.SpanArgument sarg = (EventMention.SpanArgument) arg;
        addTokensAndOffsets(argXml, sarg);
      }
      argXml.setAttribute("id", idStringByIdentity(arg));
      return argXml;
    }

    private Element actorMentionsToXML(final Document xmlDoc, final ActorMentions actorMentions,
        String sentenceTheoryID) {
      final Element ret = xmlDoc.createElement(SerifXML.ACTOR_MENTION_SET_ELEMENT);

      setElementIdByObjectByIdentity(ret, actorMentions);
      for (final ActorMention am : actorMentions) {
        ret.appendChild(actorMentionsToXML(xmlDoc, am, sentenceTheoryID));
      }

      return ret;
    }

    private Element actorMentionsToXML(final Document xmlDoc, final ActorMention am, String sentenceTheoryID) {
      final Element ret = xmlDoc.createElement(SerifXML.ACTOR_MENTION_ELEMENT);
      setElementIdByObjectByIdentity(ret, am);
      ret.setAttribute("sentence_theory_id", sentenceTheoryID);
      ret.setAttribute("mention_id", idStringByIdentity(am.mention()));
      ret.setAttribute("source_note", am.sourceNote().toString());
      if (am.actorName().isPresent()) {
        ret.setAttribute("actor_name", am.actorName().get().toString());
      }

      if (am instanceof ProperNounActorMention) {
        if (((ProperNounActorMention) am).actorPatternID().isPresent())
          ret.setAttribute("actor_pattern_uid",Long.toString(((ProperNounActorMention) am).actorPatternID().get()));
        ret.setAttribute("actor_uid", Long.toString(((ProperNounActorMention) am).actorID()));
        if (((ProperNounActorMention) am).associationScore() != ProperNounActorMention.DEFAULT_ASSOCIATION_SCORE) {
          ret.setAttribute("association_score",
              Double.toString(((ProperNounActorMention) am).associationScore()));
        }
        if (((ProperNounActorMention) am).patternConfidenceScore() != ProperNounActorMention.DEFAULT_PATTERN_CONFIDENCE_SCORE) {
          ret.setAttribute("pattern_confidence_score",
              Double.toString(((ProperNounActorMention) am).patternConfidenceScore()));
        }
        if (((ProperNounActorMention) am).patternMatchScore() != ProperNounActorMention.DEFAULT_PATTERN_MATCH_SCORE) {
          ret.setAttribute("pattern_match_score",
              Double.toString(((ProperNounActorMention) am).patternMatchScore()));
        }
        if (((ProperNounActorMention) am).editDistanceScore() != ProperNounActorMention.DEFAULT_EDIT_DISTANCE_SCORE) {
          ret.setAttribute("edit_distance_score",
              Double.toString(((ProperNounActorMention) am).editDistanceScore()));
        }
        if (((ProperNounActorMention) am).geoResolutionScore() != ProperNounActorMention.DEFAULT_GEO_RESOLUTION_SCORE) {
          ret.setAttribute("georesolution_score",
              Double.toString(((ProperNounActorMention) am).geoResolutionScore()));
        }
        if (((ProperNounActorMention) am).importanceScore() != ProperNounActorMention.DEFAULT_IMPORTANCE_SCORE) {
          ret.setAttribute("importance_score",
              Double.toString(((ProperNounActorMention) am).importanceScore()));
        }
        GeoResolvedActor geo = ((ProperNounActorMention) am).geoResolvedActor().orNull();
        if (geo != null) {
          Symbol geoText = geo.geoText().orNull();
          if (geoText != null) {
            ret.setAttribute("geo_text", geoText.toString());
          }
          Symbol geoCountry = geo.geoCountry().orNull();
          if (geoCountry != null) {
            ret.setAttribute("geo_country", geoCountry.toString());
          }
          if (geo.geoID().isPresent()) {
            ret.setAttribute("geo_uid", geo.geoID().get().toString());
          } else {
            ret.setAttribute("geo_uid", "");
          }
          if (geo.geoLatitude().isPresent()) {
            ret.setAttribute("geo_latitude", Double.toString(geo.geoLatitude().get()));
          }
          if (geo.geoLongitude().isPresent()) {
            ret.setAttribute("geo_longitude", Double.toString(geo.geoLongitude().get()));
          }
          if (geo.countryInfo().isPresent()) {
            GeoResolvedActor.CountryInfo countryInfo = geo.countryInfo().get();
            ret.setAttribute("country_id", Long.toString(countryInfo.countryID()));
            ret.setAttribute("iso_code", countryInfo.isoCode().toString());
            ret.setAttribute("country_info_actor_id",
                Long.toString(countryInfo.countryInfoActor().actorID()));
            if (countryInfo.countryInfoActor().databaseKey().isPresent()) {
              ret.setAttribute("actor_db_name",
                  countryInfo.countryInfoActor().databaseKey().get().toString());
            }
            if (countryInfo.countryInfoActorCode().isPresent()) {
              ret.setAttribute("country_info_actor_code",
                  countryInfo.countryInfoActorCode().get().toString());
            }
          }
        }
        if (am.actorDBName().isPresent()){
          ret.setAttribute("actor_db_name", am.actorDBName().get().asString());
        }
        if(((ProperNounActorMention) am).isAcronym().isPresent()) {
          ret.setAttribute("is_acronym", Boolean.toString(
              ((ProperNounActorMention) am).isAcronym().get()).toUpperCase());
        }
        if(((ProperNounActorMention) am).requiresContext().isPresent()) {
          ret.setAttribute("requires_context", Boolean.toString(
              ((ProperNounActorMention) am).requiresContext().get()).toUpperCase());
        }
      } else if (am instanceof CompositeActorMention) {
        ret.setAttribute("paired_agent_uid",
            Long.toString(((CompositeActorMention) am).pairedAgentID()));
        ret.setAttribute("paired_agent_code",
            ((CompositeActorMention) am).pairedAgentCode().toString());
        if (((CompositeActorMention) am).pairedAgentPatternID().isPresent()){
          ret.setAttribute("paired_agent_pattern_uid",
              Long.toString(((CompositeActorMention) am).pairedAgentPatternID().get()));
        }
        if (((CompositeActorMention) am).pairedAgentName().isPresent()) {
          ret.setAttribute("paired_agent_name",
              ((CompositeActorMention) am).pairedAgentName().get().asString());
        }
        if (((CompositeActorMention) am).pairedActorID().isPresent()) {
          ret.setAttribute("paired_actor_uid",
              Long.toString(((CompositeActorMention) am).pairedActorID().get()));
        }
        if (((CompositeActorMention) am).pairedActorCode().isPresent()) {
          ret.setAttribute("paired_actor_code",
              ((CompositeActorMention) am).pairedActorCode().get().toString());
        }
        if (((CompositeActorMention) am).pairedActorPatternID().isPresent()) {
          ret.setAttribute("paired_actor_pattern_uid",
              Long.toString(((CompositeActorMention) am).pairedActorPatternID().get()));
        }
        if (((CompositeActorMention) am).pairedActorName().isPresent()) {
          ret.setAttribute("paired_actor_name",
              ((CompositeActorMention) am).pairedActorName().get().asString());
        }
        if (((CompositeActorMention) am).actorAgentPattern().isPresent()) {
          ret.setAttribute("actor_agent_pattern",
              ((CompositeActorMention) am).actorAgentPattern().get().toString());
        }
        if (am.actorDBName().isPresent()){
          ret.setAttribute("actor_db_name", am.actorDBName().get().asString());
        }
      }
      return ret;
    }

    private Element toXML(final Document xmlDoc, final RelationMention relMention) {
      final Element ret = xmlDoc.createElement(SerifXML.RELATION_MENTION_ELEMENT);

      setElementIdByObjectByIdentity(ret, relMention);
      ret.setAttribute("score", Double.toString(relMention.score()));
      ret.setAttribute("left_mention_id", idStringByIdentity(relMention.leftMention()));
      ret.setAttribute("right_mention_id", idStringByIdentity(relMention.rightMention()));
      ret.setAttribute("modality", relMention.modality().name().toString());
      ret.setAttribute("tense", relMention.tense().name().toString());
      ret.setAttribute("type", relMention.type().toString());
      if(relMention.model().isPresent()){
        ret.setAttribute("model",relMention.model().get());
      }
      if(relMention.pattern().isPresent()){
        ret.setAttribute("pattern",relMention.pattern().get());
      }
      writeExternalID(relMention, ret);

      return ret;
    }

    private <T> String objectsToIdsByIdentity(final Iterable<T> objects) {
      return joinFunction(spaceJoiner()).apply(
          Iterables.transform(objects, new Function<T, String>() {
            @Override
            public String apply(final T mention) {
              return idStringByIdentity(mention);
            }
          }));
    }

    private void setOptionalEmptySymbolAttribute(final Element e, final String attName,
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<Symbol> attribute) {
      if (attribute.isPresent() && !attribute.get().toString().equals("")) {
        e.setAttribute(attName, attribute.get().toString());
      }
    }

    private <T> void setOptionalIdAttributeByIdentity(final Element e, final String attName,
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") final Optional<T> attribute) {
      if (attribute.isPresent()) {
        e.setAttribute(attName, idStringByIdentity(attribute.get()));
      }
    }

    private void addTokensAndOffsets(final Element e, final Spanning spanning) {
      final Span span = spanning.span();
      e.setAttribute("start_token", idStringByIdentity(span.startToken()));
      e.setAttribute("end_token", idStringByIdentity(span.endToken()));

      addOffsets(e, spanning);
    }

    private void addOffsets(final Element e, final Spanning spanning) {
      final Span span = spanning.span();

      e.setAttribute(
          SerifXML.CHAR_OFFSETS_ATTRIBUTE,
          String.format("%d:%d", span.startToken().startCharOffset()
              .asInt(), span.endToken().endCharOffset().asInt()));
      e.setAttribute(
          SerifXML.EDT_OFFSETS_ATTRIBUTE,
          String.format("%d:%d", span.startToken().startEDTOffset()
              .asInt(), span.endToken().endEDTOffset().asInt()));
    }

    private void addOffsets(final Element e, final LocatedString ls) {
      checkNotNull(ls);
      addOffsets(e, ls.referenceBounds());
    }

    private void addOffsets(final Element e, final OffsetGroupRange bounds) {
      checkNotNull(e);
      checkNotNull(bounds);

      e.setAttribute(
          SerifXML.CHAR_OFFSETS_ATTRIBUTE,
          String.format("%d:%d", bounds.startInclusive().charOffset().asInt(),
              bounds.endInclusive().charOffset().asInt())
      );
      e.setAttribute(
          SerifXML.EDT_OFFSETS_ATTRIBUTE,
          String.format("%d:%d", bounds.startInclusive().edtOffset().asInt(),
              bounds.endInclusive().edtOffset().asInt())
      );
      if (bounds.startInclusive().byteOffset().isPresent() && bounds.endInclusive().byteOffset()
          .isPresent()) {
        e.setAttribute(
            SerifXML.BYTE_OFFSETS_ATTRIBUTE,
            String.format("%d:%d", bounds.startInclusive().byteOffset().get().asInt(),
                bounds.endInclusive().byteOffset().get().asInt())
        );
      }
      // TODOs copied from C++ SERIF, which doesn't do them either
      // [XX] TODO: deal with ASR offsets.
      // [XX] TODO: deal with byte offsets if the input wasn't utf-8
    }

    private void setElementIdByObjectByIdentity(final Element e, final Object o) {
      e.setAttribute("id", idStringByIdentity(o));
    }

   /* private void setElementLocatedStringAttributes(final Document xmlDoc, final Element e,
        final LocatedString locString) {
      setElementLocatedStringAttributes(xmlDoc, e, locString, false);
    }*/

    private void setElementLocatedStringAttributes(final Document xmlDoc, final Element e,
        final LocatedString locString,
        @SuppressWarnings("OptionalUsedAsFieldOrParameterType") LocatedString originalText) {
      addOffsets(e, locString);

      if (!locString.equals(originalText) && originalText.containsExactly(locString)) {
        // If the original text has been serialized, and this LocatedString is
        // a proper substring of the original text, then we're done -- we can
        // reconstruct the string from just the offsets.
        return;
      }

      final Element contentElement = xmlDoc.createElement(SerifXML.CONTENT_ELEMENT);
      contentElement.setTextContent(locString.content().utf16CodeUnits());
      e.appendChild(contentElement);

      for (final LocatedString.CharacterRegion entry : locString.characterRegions()) {
        final Element offsetSpanElement = xmlDoc.createElement(SerifXML.OFFSET_SPAN_ELEMENT);
        final OffsetGroupRange entryRange =
            OffsetGroupRange.from(entry.referenceStartOffsetInclusive(),
                entry.referenceEndOffsetInclusive());
        addOffsets(offsetSpanElement, entryRange);
        offsetSpanElement
            .setAttribute(SerifXML.START_POS_ATTRIBUTE,
                Integer.toString(entry.contentStartPosInclusive().asInt()));
        offsetSpanElement.setAttribute(SerifXML.END_POS_ATTRIBUTE,
            Integer.toString(entry.contentEndPosExclusive().asInt()));
        e.appendChild(offsetSpanElement);
      }
    }

    private void assignIdByIdentity(final Object o, final String id) {
      idMapByIdentity.put(o, id);
    }

    private String idStringByIdentity(final Object o) {
      String id = idMapByIdentity.get(o);

      if (id == null) {
        id = "a" + (nextId++);
        idMapByIdentity.put(o, id);
      }

      return id;
    }

    private String idStringByValue(final Object o) {
      String id = idMapByValue.get(o);

      if (id == null) {
        id = "a" + (nextId++);
        idMapByValue.put(o, id);
      }

      return id;
    }

    private Function<Object, String> idStringByValueFunction() {
      return new Function<Object, String>() {
        @Override
        public String apply(final Object input) {
          return idStringByValue(input);
        }
      };
    }

    private final Map<Object, String> idMapByIdentity = new IdentityHashMap<>();
    private final Map<Object, String> idMapByValue = new HashMap<>();
    private int nextId = 1;
  }

  private static String capsBoolean(final boolean b) {
    return b ? "TRUE" : "FALSE";
  }


  // package-private for use by SerifXMLLoader
  static String pseudoParseKey(final TokenSequence ts) {
    return "pseudo-parse-" + ts.sentenceIndex();
  }
}
