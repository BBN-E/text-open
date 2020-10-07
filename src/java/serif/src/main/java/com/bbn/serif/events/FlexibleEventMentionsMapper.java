package com.bbn.serif.events;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.EventMentions;
import com.bbn.serif.theories.Events;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.TokenSpan;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMention;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMentionArgument;
import com.bbn.serif.types.Genericity;
import com.bbn.serif.types.Modality;
import com.bbn.serif.types.Polarity;
import com.bbn.serif.types.Tense;

import com.google.common.collect.ImmutableList;

public final class FlexibleEventMentionsMapper {

  private static final Symbol ANCHOR = Symbol.from("Anchor");
  private static final Symbol FUTURE = Symbol.from("Future");
  private static final Symbol HYPOTHETICAL = Symbol.from("Hypothetical");
  private static final Symbol MODALITY = Symbol.from("Modality");

  private FlexibleEventMentionsMapper() {}

  public static FlexibleEventMentionsMapper createForCivilUnrest() {
    return new FlexibleEventMentionsMapper();
  }

  public DocTheory projectFlexibleEventMentionsToRegularEventMentions(final DocTheory oldDocTheory) {
    final DocTheory.Builder newDocTheory = oldDocTheory.modifiedCopyBuilder();
    newDocTheory.events(Events.absent());  // Clear our events as we are replacing them
    for (int sentIndex = 0; sentIndex < oldDocTheory.numSentences(); sentIndex++) {
      final SentenceTheory st = oldDocTheory.sentenceTheory(sentIndex);
      final SentenceTheory.Builder newSentence = st.modifiedCopyBuilder();
      final ImmutableList.Builder<EventMention> newEventMentions = ImmutableList.builder();
      // Loop over our flexible event mentions and see if any match
      for (final FlexibleEventMention flex : oldDocTheory.flexibleEventMentions()) {
        // Set our genericity, modality, polarity, and tense
        final Genericity genericity = Genericity.SPECIFIC;
        final Modality modality;  // Set below based on attributes
        final Polarity polarity = Polarity.POSITIVE;
        final Tense tense;  // Set below based on attributes
        if (flex.attributes().containsKey(MODALITY) &&
            flex.attributes().get(MODALITY).equalTo(HYPOTHETICAL)) {
          modality = Modality.OTHER;
        } else {
          modality = Modality.ASSERTED;
        }
        if (flex.attributes().containsKey(MODALITY) &&
            flex.attributes().get(MODALITY).equalTo(FUTURE)) {
          tense = Tense.FUTURE;
        } else {
          tense = Tense.UNSPECIFIED;
        }
        // Check if we have an anchor in this sentence
        ImmutableList<FlexibleEventMentionArgument> anchors = flex.getArgumentsByRoleAndSentenceIndex(
            ANCHOR, sentIndex);
        ImmutableList<FlexibleEventMentionArgument> nonAnchors =
        		flex.getArgumentsBySentenceIndexExcludingRole(sentIndex,ANCHOR);
        for (FlexibleEventMentionArgument anchor : anchors) {
          final SynNode anchorNode;  // Trigger model operates over tokens, so we want the head
          if (anchor.mention().isPresent()) {
            anchorNode = ((FlexibleEventMentionArgument) anchor).mention().get().node().headPreterminal();
          } else if (anchor.synNode().isPresent()) {
            anchorNode = ((FlexibleEventMentionArgument) anchor).synNode().get().headPreterminal();
          } else {
            throw new SerifException("Anchor argument of unhandled type: " + anchor.getClass());
          }
          // Construct an event mention for this anchor and any arguments in the same sentence
          // Non-anchor arguments can be full SynNodes of arbitrary length
          final ImmutableList.Builder <EventMention.Argument> args = ImmutableList.builder();
          for (final FlexibleEventMentionArgument nonAnchor : nonAnchors) {
            if (nonAnchor.mention().isPresent()) {
              final Mention mention = nonAnchor.mention().get();
              args.add(EventMention.MentionArgument.from(nonAnchor.role(), mention, 1.0f));
            } else if (nonAnchor.valueMention().isPresent()) {
              final ValueMention valueMention = nonAnchor.valueMention().get();
              args.add(
                  EventMention.ValueMentionArgument.from(nonAnchor.role(), valueMention, 1.0f));
            } else if (nonAnchor.synNode().isPresent()) {
              final SynNode synNode = nonAnchor.synNode().get();
              // As there is currently no EventMention.SynNodeArgument class, we do the somewhat
              // absurd thing of turning our SynNode into a SpanArgument
              args.add(EventMention.SpanArgument.from(nonAnchor.role(), synNode.span(), 1.0f));
            } else if (!nonAnchor.synNode().isPresent()) {//i.e., non-anchor is a TokenSpanArgument
              final TokenSpan tokenSpan = nonAnchor.tokenSpan();
              if (tokenSpan.inSingleSentence()) {  // Handle single sentence spans
                final TokenSequence tokenSequence = oldDocTheory.sentenceTheory(tokenSpan.startSentenceIndex()).tokenSequence();
                final TokenSequence.Span span =
                    tokenSequence.span(tokenSpan.startTokenIndexInclusive(), tokenSpan.endTokenIndexInclusive());
                args.add(EventMention.SpanArgument.from(nonAnchor.role(), span, 1.0f));
              } else {  // Handle multi-sentence spans
                final TokenSequence firstTokenSequence = oldDocTheory.sentenceTheory(tokenSpan.startSentenceIndex()).tokenSequence();
                final TokenSequence.Span firstSpan =
                    firstTokenSequence.span(tokenSpan.startTokenIndexInclusive(), firstTokenSequence.size() - 1);
                args.add(EventMention.SpanArgument.from(nonAnchor.role(), firstSpan, 1.0f)); // First sentence
                for (int intSentIndex = tokenSpan.startSentenceIndex() + 1; intSentIndex < tokenSpan.endSentenceIndex(); intSentIndex++) {
                  final TokenSequence intTokenSequence = oldDocTheory.sentenceTheory(intSentIndex).tokenSequence();
                  TokenSequence.Span intSpan = intTokenSequence.span();
                  args.add(EventMention.SpanArgument.from(nonAnchor.role(), intSpan, 1.0f)); // Intermediate sentences
                }
                final TokenSequence lastTokenSequence = oldDocTheory.sentenceTheory(tokenSpan.endSentenceIndex()).tokenSequence();
                final TokenSequence.Span lastSpan = lastTokenSequence.span(0, tokenSpan.endTokenIndexInclusive());
                args.add(EventMention.SpanArgument.from(nonAnchor.role(), lastSpan, 1.0f)); // Last sentence
              }

            } else {
              throw new SerifException("Non-anchor argument of unhandled type: " + nonAnchor.getClass());
            }
          }
          final EventMention em = EventMention.builder(flex.type())
              .setAnchorNode(anchorNode)
              .setAnchorPropFromNode(anchorNode.sentenceTheory(oldDocTheory))
              .setArguments(args.build())
              .setGenericity(genericity, 1.0f)
              .setModality(modality, 1.0f)
              .setPolarity(polarity)
              .setTense(tense)
              .build();
          newEventMentions.add(em);
        }
      }
      newSentence.eventMentions(
          new EventMentions.Builder().eventMentions(newEventMentions.build()).build());
      newDocTheory.replacePrimarySentenceTheory(st, newSentence.build());
    }
    return newDocTheory.build();
  }

}
