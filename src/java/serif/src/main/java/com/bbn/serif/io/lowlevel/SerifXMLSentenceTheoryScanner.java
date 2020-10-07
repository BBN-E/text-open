package com.bbn.serif.io.lowlevel;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A low-level parser for pulling selected information out of a sentence theory's XML fragment
 * without having to parse everything. Mostly useful for Hadoop jobs. This code favors speed over
 * robustness, and should probably not be used for anything but BBN-internal experiments.
 */
public final class SerifXMLSentenceTheoryScanner {

  private final String xml;

  private SerifXMLSentenceTheoryScanner(final String xml) {
    this.xml = checkNotNull(xml);
    checkArgument(xml.startsWith("<Sentence "));
    checkArgument(xml.endsWith("</Sentence>"));
  }

  /**
   * Creates a sentence theory scanner from an SentenceTheory XML fragment. This must begin
   * *exactly* with "<SentenceTheory" and end with "</SentenceTheory>" or an exception will be
   * thrown.
   */
  public static SerifXMLSentenceTheoryScanner fromSentenceTheoryFragment(final String xmlFragment) {
    return new SerifXMLSentenceTheoryScanner(xmlFragment);
  }

  /**
   * Get a list of the tokens in this sentence.  The previous contents of {@code tokens} will be
   * cleared and replaced with the output to avoid allocating a new list for every sentence.
   */
  public void extractTokens(final List<String> tokens) {
    tokens.clear();
    // <Token char_offsets="285:285" edt_offsets="174:174" id="a39">-LRB-</Token>

    for (int idx = xml.indexOf("</Token>"); idx >= 0; idx = xml.indexOf("</Token>", idx + 1)) {
      final int previousGreaterThan = xml.lastIndexOf('>', idx);
      if (previousGreaterThan >= 0) {
        tokens.add(xml.substring(previousGreaterThan + 1, idx));
      } else {
        throw new RuntimeException(String.format("Malformed token element: %s", xml));
      }
    }
  }

  /**
   * Get a list of the event mention types in this sentence.  The previous contents of {@code
   * eventMentionTypes} will be cleared and replaced with the output to avoid allocating a new list
   * for every sentence. If there are multiple event mentions of the same type, the event mention
   * type will appear multiple times in the output.
   */
  public void extractEventMentionTypesPresent(final List<String> eventMentionTypes) {
    eventMentionTypes.clear();
    // <EventMention anchor_node_id="a94.16" anchor_prop_id="a112" event_type="Life.Die"
    //    genericity="Specific" id="a130" modality="Asserted" polarity="Positive" score="0" tense="Unspecified">
    final String eventMentionStart = "<EventMention ";
    final String eventTypeStart = "event_type=\"";
    for (int idx = xml.indexOf(eventMentionStart); idx >= 0;
         idx = xml.indexOf(eventMentionStart, idx + 1)) {
      // first find "<EventMention ", then scan for event_type
      final int eventTypeIdx = xml.indexOf(eventTypeStart, idx);
      if (eventTypeIdx >= 0) {
        final int typeStartIdx = eventTypeIdx + eventTypeStart.length();
        final int closingQuote = xml.indexOf('"', typeStartIdx);
        if (closingQuote >= 0) {
          eventMentionTypes.add(xml.substring(typeStartIdx, closingQuote));
        } else {
          throw new RuntimeException(
              String.format("Error while searching for closing quote: %s", xml));
        }
      } else {
        throw new RuntimeException(
            String.format("Error while searching for event_type attribute: %s", xml));
      }
    }
  }
}
