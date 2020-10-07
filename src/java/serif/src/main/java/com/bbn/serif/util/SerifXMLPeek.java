package com.bbn.serif.util;

import com.bbn.bue.common.strings.LocatedString;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.io.SerifXMLLoader;
import com.bbn.serif.theories.DocTheory;
import com.bbn.serif.theories.DocumentEvent;
import com.bbn.serif.theories.Entity;
import com.bbn.serif.theories.EventMention;
import com.bbn.serif.theories.Mention;
import com.bbn.serif.theories.Name;
import com.bbn.serif.theories.Parse;
import com.bbn.serif.theories.Proposition;
import com.bbn.serif.theories.SentenceTheory;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;
import com.bbn.serif.theories.Value;
import com.bbn.serif.theories.ValueMention;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMention;
import com.bbn.serif.theories.flexibleevents.FlexibleEventMentionArgument;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public final class SerifXMLPeek {

  private static void usage() {
    System.out.format("usage:\nSerifXMLPeek peekMode file args\n" +
        "where valid peek modes are %s\n", peekers.keySet());
    System.exit(1);
  }

  public static void main(String[] argv) throws IOException {
    final List<String> argList = Arrays.asList(argv);
    if (argList.size() < 2) {
      usage();
      System.exit(1);
    }

    final String mode = argList.get(0);
    final Peeker peeker = peekers.get(mode);

    if (peeker != null) {
      final SerifXMLLoader loader =
          SerifXMLLoader.builder().build();
      try {
        peeker.peek(loader.loadFrom(new File(argList.get(1))), argList.subList(2, argList.size()));
      } catch (Exception e) {
        System.out.format("usage: SerifXMLPeek %s fileName %s\n", mode, peeker.usage());
        e.printStackTrace();
        System.exit(1);
      }
    } else {
      System.out
          .format("Invalid peek mode %s. Valid modes are %s\n", argList.get(0), peekers.keySet());
      System.exit(1);
    }
  }

  private interface Peeker {

    public void peek(DocTheory dt, List<String> args);

    public String usage();
  }

  private static final Peeker charOffsetPeeker = new Peeker() {
    @Override
    public void peek(DocTheory dt, List<String> args) {
      final int firstOffset = Integer.parseInt(args.get(0));
      final int secondOffset = Integer.parseInt(args.get(1));

      System.out.println(dt.document().originalText()
          .referenceString().get().substringByCodePoints(OffsetRange.charOffsetRange(
              firstOffset, secondOffset)));
    }

    @Override
    public String usage() {
      return "start_char_offset end_char_offset";
    }
  };

  private static final Peeker sentenceOffsetPeeker = new Peeker() {
    @Override
    @SuppressWarnings("deprecation")
    public void peek(DocTheory dt, List<String> args) {
      final int sentenceIdx = Integer.parseInt(args.get(0));

      final TokenSequence.Span span = dt.sentenceTheory(sentenceIdx).span();
      System.out.format("char_offsets: %d-%d\n", span.startCharOffset().value(),
          span.endCharOffset().value());
    }

    @Override
    public String usage() {
      return "sentence_index";
    }
  };

  private static final Peeker mentionOffsetPeeker = new Peeker() {
    @Override
    @SuppressWarnings("deprecation")
    public void peek(DocTheory dt, List<String> args) {
      final int sentenceIdx = Integer.parseInt(args.get(0));
      final int mentionIdx = Integer.parseInt(args.get(1));

      final TokenSequence.Span span =
          dt.sentenceTheory(sentenceIdx).mentions().get(mentionIdx).span();
      System.out.format("char_offsets: %d-%d\n", span.startCharOffset().value(),
          span.endCharOffset().value());
    }

    @Override
    public String usage() {
      return "sentence_index mention_index";
    }
  };


  private static final Peeker valueMentionOffsetPeeker = new Peeker() {
    @Override
    @SuppressWarnings("deprecation")
    public void peek(DocTheory dt, List<String> args) {
      final int sentenceIdx = Integer.parseInt(args.get(0));
      final int vmIdx = Integer.parseInt(args.get(1));

      final TokenSequence.Span span =
          dt.sentenceTheory(sentenceIdx).valueMentions().get(vmIdx).span();
      System.out.format("char_offsets: %d-%d\n", span.startCharOffset().value(),
          span.endCharOffset().value());
    }

    @Override
    public String usage() {
      return "sentence_index value_mention_index";
    }
  };

  private static final Peeker sentenceTheoryAndMentionPeeker = new Peeker() {

    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      System.out.printf("%-30s: mentions\n", "token");
      for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
        final TokenSequence ts = st.tokenSequence();
        for (final Token t : ts) {
          System.out.printf("%-30s: ", t.tokenizedText());
          for (final Mention m : st.mentions()) {
            if (m.head().span().contains(st.tokenSequence().span(t))) {
              System.out.print(m.tokenSpan().originalText().content().utf16CodeUnits() + "\t");
            }
          }
          System.out.println();
        }
      }
    }

    @Override
    public String usage() {
      return "";
    }
  };

  private static final Peeker parseMentionNamePeeker = new Peeker() {
    final String space = "  ";

    private String mention(Mention m) {
      return m.entityType() + "/" + m.entitySubtype() + "/" + m.mentionType() + ":" + m.tokenSpan()
          .originalText().content().utf16CodeUnits();
    }

    private void peek(final SentenceTheory st, final int n) {
      checkArgument(st.parse().root().isPresent());
      System.out.println(n + ":");
      final Parse parse = st.parse();
      // DFS
      final List<SynNode> nodes = Lists.newArrayList();
      nodes.add(parse.root().get());
      while (!nodes.isEmpty()) {
        final SynNode current = nodes.remove(nodes.size() - 1);
        nodes.addAll(current.children());
        // node
        final int indent = current.ancestorDistance(parse.root().get()).or(0) + 1;
        System.out.println(Strings.repeat(space, indent) + current);
        // mention, if any
        if (current.hasMention()) {
          System.out.println(Strings.repeat(space, indent + 1) + "ment: " + mention(
              current.mention().get()));
        }
        // name, if any
        for (final Name name : st.names()) {
          if (current.tokenSpan().contains(name.tokenSpan())) {
            System.out.println(
                Strings.repeat(space, indent + 1) + "name: " + name.tokenSpan().originalText()
                    .content().utf16CodeUnits() + ": " + name.type());
          }
        }
      }
    }

    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      if (args.size() == 0) {
        int n = 0;
        for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
          peek(st, n);
          n++;
        }
      } else {
        final int n = Integer.parseInt(args.get(0));
        final SentenceTheory st = ImmutableList.copyOf(dt.nonEmptySentenceTheories()).get(n);
        peek(st, n);
      }
    }

    @Override
    public String usage() {
      return "[sentence_index]";
    }
  };

  private static final Peeker originalTextPeeker = new Peeker() {

    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      System.out.println(dt.document().originalText());
    }

    @Override
    public String usage() {
      return "";
    }
  };

  private static final Peeker namePeeker = new Peeker() {
    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
        for (final Name name : st.names()) {
          System.out.println(name.tokenSpan().originalText().content().utf16CodeUnits());
        }
      }
    }

    @Override
    public String usage() {
      return "";
    }
  };

  private static final Peeker mentionPeeker = new Peeker() {
    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      checkArgument(args.size() <= 1);
      if (args.size() == 1) {
        final SentenceTheory st = dt.sentenceTheory(Integer.parseInt(args.get(0)));
        printMentions(st);
      } else {
        for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
          printMentions(st);
        }
      }
    }

    private void printMentions(final SentenceTheory st) {
      for (final Mention m : st.mentions()) {
        final String mention_text = m.tokenSpan().originalText().content().utf16CodeUnits();
        System.out.println(mention_text);
      }
    }

    @Override
    public String usage() {
      return "[sentence_index]";
    }
  };

  private static Peeker allEventsPeeker = new Peeker() {
    private String indent(int level) {
      return Strings.repeat("\t", level);
    }

    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      System.out.println("Document Events");
      for (final DocumentEvent de : dt.documentEvents()) {
        print(de);
      }
      // TODO document event events
      System.out.println("Flexible Event Mentions");
      for (final FlexibleEventMention flexEM : dt.flexibleEventMentions()) {
        print(flexEM, 1);
      }
      // TODO ICEWS
      System.out.println("SentenceTheory Events");
      for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
        print(st);
      }
    }

    private void print(final DocumentEvent de) {
      final String pattern = "%s\t%s\t%s";
      final String score = de.score().isPresent() ? String.valueOf(de.score().get()) : "??";
      final String externalID =
          de.externalID().isPresent() ? de.externalID().get().asString() : "??";
      System.out.println(String.format(pattern, de.primaryType(), score, externalID));
      System.out.println("\tJustifications:");
      for (final Symbol k : de.justifications().keys()) {
        System.out.println("\t\t" + k.asString());
        for (final LocatedString ls : de.justifications().get(k)) {
          System.out.println("\t\t\t" + ls);
        }
      }
      System.out.println("\tProvenances:");
      for (final DocumentEvent.Provenance prov : de.provenances()) {
        print(prov, 2);
      }
      System.out.println("\tArguments");
      for (final DocumentEvent.Argument arg : de.arguments()) {
        print(arg, 2);
      }

      System.out.println("\tScored Attributes:");
      printMap(de.scoredAttributes(), 2);
    }

    private void printMap(final ImmutableMap<Symbol, ?> map, final int i) {
      for (final Map.Entry<Symbol, ?> e : map.entrySet()) {
        System.out.println(indent(i) + e.getKey() + "\t=>\t" + e.getValue());
      }
    }

    private void print(final DocumentEvent.Provenance prov, final int indent) {
      if (prov instanceof DocumentEvent.EventMentionProvenance) {
        print(((DocumentEvent.EventMentionProvenance) prov).eventMention(), indent);
      } else if (prov instanceof DocumentEvent.FlexibleEventMentionProvenance) {
        print(((DocumentEvent.FlexibleEventMentionProvenance) prov).flexibleEventMention(), indent);
      } else if (prov instanceof DocumentEvent.TextualProvenance) {
        print(((DocumentEvent.TextualProvenance) prov).locatedStrings(), indent);
      } else {
        throw new SerifException("Unrenderable provenance " + prov.getClass());
      }
    }

    private void print(final Iterable<LocatedString> locatedStrings, final int indent) {
      for (final LocatedString ls : locatedStrings) {
        System.out.println(indent(indent) + ls);
      }
    }

    private void print(final DocumentEvent.Argument arg, final int indent) {
      final String type = arg.type().asString();
      final String role = arg.role().asString();
      final String score = arg.score().isPresent() ? String.valueOf(arg.score().get()) : "??";
      final String externalID =
          arg.externalID().isPresent() ? arg.externalID().get().asString() : "??";

      final DocumentEvent.Argument.ArgumentFiller filler = arg.filler();
      System.out.println(indent(indent) + String
          .format("%s/%s\t%s\t%s:", type, role, score, externalID));
      System.out.println(indent(indent) + "Filler:");
      print(filler, indent + 1);
      System.out.println(indent(indent) + "Provenances:");
      for (final DocumentEvent.Argument.ArgumentProvenance argumentProvenance : arg.provenances()) {
        print(argumentProvenance, indent + 1);
      }
      System.out.println(indent(indent) + "Scored Attributes");
      printMap(arg.scoredAttributes(), indent + 1);
      System.out.println(indent(indent) + "Metadata");
      printMap(arg.metadata(), indent + 1);
      System.out.println(indent(indent) + "Justifications");
      for (final Symbol k : arg.justifications().keySet()) {
        System.out.println(indent(indent + 1) + k);
        print(arg.justifications().get(k), indent + 2);
      }
    }


    private void print(final DocumentEvent.Argument.ArgumentFiller filler, final int indent) {
      if (filler.canonicalString().isPresent()) {
        System.out.println(indent(indent) + filler.canonicalString().get());
      }
      if (filler.canonicalStringOffsets().isPresent()) {
        System.out.println(indent(indent) + filler.canonicalStringOffsets().get());
      }
      if (filler instanceof DocumentEvent.EntityFiller) {
        print(((DocumentEvent.EntityFiller) filler).entity(), indent);
      } else if (filler instanceof DocumentEvent.TextFiller) {
        print(ImmutableList.of(((DocumentEvent.TextFiller) filler).text()), indent);
      } else if (filler instanceof DocumentEvent.ValueFiller) {
        print(((DocumentEvent.ValueFiller) filler).value(), indent);
      } else if (filler instanceof DocumentEvent.ValueMentionFiller) {
        print(((DocumentEvent.ValueMentionFiller) filler).valueMention(), indent);
      } else {
        throw new SerifException("Unrenderable filler " + filler.getClass());
      }
    }

    private void print(final ValueMention valueMention, final int indent) {
      System.out.println(indent(indent) + valueMention.toStringNoValue());
    }

    private void print(final Value value, final int indent) {
      System.out.println(indent(indent) + value.toString());
    }

    private void print(final Entity entity, final int indent) {
      final String type = entity.type().toString() + "." + entity.subtype().toString();
      final String guid = entity.guid().isPresent() ? String.valueOf(entity.guid().get()) : "??";
      final String externalID =
          entity.externalID().isPresent() ? entity.externalID().get().asString() : "??";
      System.out.println(indent(indent) + String.format("%s\t%s\t%s", type, guid, externalID));
      for (final Mention m : entity.mentions()) {
        print(m, indent + 1);
      }
    }

    private void print(final Mention m, final int i) {
      System.out.println(indent(i) + m.mentionType() + "\t" + m.span().tokenizedText());
    }

    private void print(final DocumentEvent.Argument.ArgumentProvenance argumentProvenance,
        final int indent) {
      if (argumentProvenance instanceof DocumentEvent.EventMentionArgumentProvenance) {
        if (((DocumentEvent.EventMentionArgumentProvenance) argumentProvenance).eventMention()
            .isPresent()) {
          print(((DocumentEvent.EventMentionArgumentProvenance) argumentProvenance).eventMention()
              .get(), indent);
        }
      }
    }

    private void print(final FlexibleEventMention flexEM, final int indent) {
      final String type = flexEM.type().asString();
      final String externalID =
          flexEM.externalID().isPresent() ? flexEM.externalID().get().asString() : "??";
      System.out.println(indent(indent) + type + "\t" + externalID);
      System.out.println(indent(indent) + "Arguments");
      for (final FlexibleEventMentionArgument arg : flexEM.arguments()) {
        print(arg, indent + 1);
      }
      System.out.println(indent(indent) + "Attributes");
      printMap(flexEM.attributes(), indent + 1);


    }

    private void print(final FlexibleEventMentionArgument arg, final int i) {
      System.out.println(indent(i) + arg.toString());
    }

    private void print(final SentenceTheory st) {
      if (st.eventMentions().size() > 0) {
        System.out.println("EventMentions");
      }
      for (final EventMention evm : st.eventMentions()) {
        print(evm, 1);
      }
    }

    private void print(final EventMention evm, final int indent) {
      System.out.println(indent(indent) + evm.toString());
    }


    @Override
    public String usage() {
      return "";
    }
  };

  private static final Peeker propositionPeeker = new Peeker() {
    @Override
    public void peek(final DocTheory dt, final List<String> args) {
      checkArgument(args.isEmpty(), "Arguments not supported");
      int sent = 0;
      for (final SentenceTheory st : dt.nonEmptySentenceTheories()) {
        final StringBuilder sb = new StringBuilder("idx: ");
        sb.append(sent);
        sb.append("\n");
        sb.append(st.tokenSequence().text());
        sb.append("\n");
        for (final Proposition proposition : st.propositions()) {
          sb.append("\t");
          sb.append(proposition.predType().name().asString());
          if (proposition.predType().isDefinitional()) {
            sb.append(" +definitional");
          }
          if (proposition.predType().isVerbal()) {
            sb.append(" +verbal");
          }
          for (final Proposition.Argument arg : proposition.args()) {
            if (arg instanceof Proposition.MentionArgument) {
              sb.append("\n\t\t");
              sb.append(arg);
            } else if (arg instanceof Proposition.TextArgument) {
              sb.append("\n\t\t");
              sb.append(arg);
            } else if (arg instanceof Proposition.PropositionArgument) {
              sb.append("\n\t\t");
              sb.append(arg);
            } else {
              throw new RuntimeException("Unknown proposition argument type: " + arg.getClass());
            }
            sb.append("\n");
          }
        }
        System.out.println(sb);
        sent++;
      }
    }

    @Override
    public String usage() {
      return "";
    }
  };

  private static final ImmutableMap<String, Peeker> peekers = ImmutableMap.<String, Peeker>builder()
      .put("lookupCharOffsets", charOffsetPeeker)
      .put("sentenceOffsets", sentenceOffsetPeeker)
      .put("mentionOffsets", mentionOffsetPeeker)
      .put("vmOffsets", valueMentionOffsetPeeker)
      .put("sentenceTheoryAndMentions", sentenceTheoryAndMentionPeeker)
      .put("parseAndMentions", parseMentionNamePeeker)
      .put("originalText", originalTextPeeker)
      .put("name", namePeeker)
      .put("mentions", mentionPeeker)
      .put("allEventsPeeker", allEventsPeeker)
      .put("propositionPeeker", propositionPeeker)
      .build();


}
