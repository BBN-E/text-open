package com.bbn.serif.parse;

import com.bbn.bue.common.collections.RangeUtils;
import com.bbn.bue.common.strings.offsets.CharOffset;
import com.bbn.bue.common.strings.offsets.OffsetRange;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.parse.constraints.OffsetBasedParseConstraints;
import com.bbn.serif.parse.constraints.ParseConstraint;
import com.bbn.serif.theories.Parse;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.Token;
import com.bbn.serif.theories.TokenSequence;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkState;


@Beta
public class FlatTreeParserFromOffsets extends AbstractSentenceParser {

  private static final Logger log = LoggerFactory.getLogger(FlatTreeParserFromOffsets.class);
  private final static Symbol X = Symbol.from("X");

  @Inject
  private FlatTreeParserFromOffsets(@TolerantP final boolean tolerant) {
    super(tolerant);
  }

  @Override
  protected Parse parseForSentence(final TokenSequence tokenSequence,
      final Set<ParseConstraint> parseConstraints) {
    final ImmutableList<OffsetBasedParseConstraints> constraintses =
        orderParseConstraints(parseConstraints);
    final TreeRangeMap<CharOffset, SynNode> rangeToSynNodes =
        terminalNodesFromSequences(tokenSequence);
    // replace SynNodes in this map with gradually larger parent nodes.
    for (final OffsetBasedParseConstraints cons : constraintses) {
      final ImmutableList<Range<CharOffset>> intersectingRanges =
          fetchIntersectingRanges(rangeToSynNodes, cons.subtreeSpan());
      final ImmutableList<SynNode> synNodesForRanges =
          synNodesForRanges(rangeToSynNodes, intersectingRanges);
      // for now just pick the first to be the head
      final SynNode.NonterminalBuilder node = SynNode.nonterminalBuilder(X);
      node.appendHead(synNodesForRanges.get(0));
      checkState(
          synNodesForRanges.get(0).span().startToken().charOffsetRange().asRange().lowerEndpoint()
              .asInt() == cons.subtreeSpan().asRange().lowerEndpoint().asInt(),
          "lower endpoint of found synnodes not compatible with constraints!");
      checkState(
          synNodesForRanges.get(synNodesForRanges.size() - 1).span().endToken().charOffsetRange()
              .asRange().upperEndpoint().asInt() == cons.subtreeSpan().asRange().upperEndpoint()
              .asInt(),
          "upper endpoint of found synnodes not compatible with contraints!");
      for (final SynNode n : synNodesForRanges.subList(1, synNodesForRanges.size())) {
        node.appendNonHead(n);
      }
      for (final Range<CharOffset> off : intersectingRanges) {
        rangeToSynNodes.remove(off);
      }
      rangeToSynNodes.put(cons.subtreeSpan().asRange(), node.build(tokenSequence));
    }

    // put all the remaining synnodes in a root synnode
    final SynNode.NonterminalBuilder root = SynNode.nonterminalBuilder(Symbol.from("ROOT"));
    final ImmutableList<SynNode> childNodes =
        ImmutableList.copyOf(rangeToSynNodes.asMapOfRanges().values());
    root.appendHead(childNodes.get(0));
    for (final SynNode child : childNodes.subList(1, childNodes.size())) {
      root.appendNonHead(child);
    }
    return Parse.create(tokenSequence, root.build(tokenSequence), 1.0f);
  }

  // get the SynNodes that correspond to a particular range
  private ImmutableList<SynNode> synNodesForRanges(
      final RangeMap<CharOffset, SynNode> rangeToSynNodes,
      final Iterable<Range<CharOffset>> intersectingRanges) {
    final ImmutableList.Builder<SynNode> ret = ImmutableList.builder();
    final Ordering<Range<CharOffset>> byLowerEndPoint =
        Ordering.natural().onResultOf(RangeUtils.<CharOffset>lowerEndPointFunction());
    for (final Range<CharOffset> off : byLowerEndPoint.immutableSortedCopy(intersectingRanges)) {
      ret.addAll(rangeToSynNodes.subRangeMap(off).asMapOfRanges().values());
    }
    return ret.build();
  }

  // get all the ranges that are a subset of this large range.
  private static ImmutableList<Range<CharOffset>> fetchIntersectingRanges(
      final RangeMap<CharOffset, SynNode> rangeToSynNodes,
      final OffsetRange<CharOffset> charOffsetOffsetRange) {
    final ImmutableList.Builder<Range<CharOffset>> ret = ImmutableList.builder();
    for (final Range<CharOffset> r : rangeToSynNodes.asMapOfRanges().keySet()) {
      charOffsetOffsetRange.asRange();
      if (charOffsetOffsetRange.asRange().encloses(r)) {
        ret.add(r);
      } else if (charOffsetOffsetRange.asRange().isConnected(r)) {
        throw new SerifException(
            "While constructing a minimal parse tree, expected offset constraints to be capable of"
                + " forming a tree, but got crossing constraints " + r + " and "
                + charOffsetOffsetRange.asRange());
      }
    }
    return ret.build();
  }

  // turns the tokens into terminal nodes, associates each of them with the token's range.
  private TreeRangeMap<CharOffset, SynNode> terminalNodesFromSequences(
      final TokenSequence tokenSequence) {
    final TreeRangeMap<CharOffset, SynNode> ret = TreeRangeMap.create();
    for (int i = 0; i < tokenSequence.size(); i++) {
      final Token t = tokenSequence.token(i);
      final SynNode terminal =
          SynNode.terminalBuilder(t.symbol()).tokenIndex(i).build(tokenSequence);
      ret.put(t.charOffsetRange().asRange(),
          SynNode.nonterminalBuilder(X).appendHead(terminal).build(tokenSequence));
    }
    return ret;
  }

  // verify we have the correct type of constraints and order them by ascending size.
  private ImmutableList<OffsetBasedParseConstraints> orderParseConstraints(
      final Iterable<ParseConstraint> parseConstraints) {
    final ImmutableList.Builder<OffsetBasedParseConstraints> ret = ImmutableList.builder();
    for (final ParseConstraint cons : parseConstraints) {
      if (cons instanceof OffsetBasedParseConstraints) {
        ret.add((OffsetBasedParseConstraints) cons);
      } else {
        throw new SerifException(
            "Can only handle constraints of type " + OffsetBasedParseConstraints.class + " but got "
                + cons);
      }
    }
    return OffsetRange.<CharOffset>byLengthOrdering()
        .onResultOf(OffsetBasedParseConstraints.subtreeFunction()).immutableSortedCopy(ret.build());
  }

  @Override
  public void finish() {
    log.info("{} {} successful projections, {} failed", this.getClass(), successful, failed);
  }

}
