package com.bbn.serif.theories;

import com.bbn.bue.common.TextGroupImmutable;
import com.bbn.bue.common.symbols.Symbol;
import com.bbn.nlp.WordAndPOS;
import com.bbn.serif.common.SerifException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import org.immutables.value.Value;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

@TextGroupImmutable
@Value.Immutable
@JsonSerialize(as = ImmutableParse.class)
@JsonDeserialize(as = ImmutableParse.class)
public abstract class Parse implements PotentiallyAbsentSerifTheory {

  /**
   * Token sequence will always be present unless {@link #isAbsent()} is true.
   */
  public abstract Optional<TokenSequence> tokenSequence();

  /**
   * The root will be missing only if this is the parse of a sentence with an empty
   * {@link TokenSequence}
   */
  public abstract Optional<SynNode> root();

  public abstract float score();

  @Value.Default
  @Override
  public boolean isAbsent() {
    return false;
  }

  @Value.Lazy
  public ImmutableList<SynNode> preterminalNodes() {
    final ImmutableList.Builder<SynNode> ret = ImmutableList.builder();
    if (root().isPresent()) {
      findPreterminalNodes(root().get(), ret);
    }
    return ret.build();
  }

  @Value.Check
  protected void check() {
    checkArgument(isAbsent() != tokenSequence().isPresent());
    if (tokenSequence().isPresent()) {
      checkArgument(tokenSequence().get().isEmpty() == !root().isPresent());
    }
  }

  public static Parse emptyParse(TokenSequence ts) {
    return new Builder()
        .tokenSequence(ts)
        .score(1.0f)
        .build();
  }

  public static Parse absent() {
    return new Parse.Builder()
        .isAbsent(true)
        .score(1.0f)
        .build();
  }


  public static Parse create(TokenSequence tokSequence, SynNode root, float score) {
    return new Builder().tokenSequence(tokSequence).root(root).score(score).build();
  }

  public static class Builder extends ImmutableParse.Builder {

  }

  public final Symbol POSForToken(Token tok) {
    return nodeForToken(tok).tag();
  }

  public final SynNode nodeForToken(Token tok) {
    if (root().isPresent()) {
      return nodeForToken(tok.index(), root().get());
    } else {
      throw new SerifException("You are looking for a token in a sentence with no tokens."
          + " Only look for tokens in their own sentences");
    }
  }

  private SynNode nodeForToken(int idx, SynNode searchRoot) {
    checkNotNull(searchRoot);
    if (searchRoot.isPreterminal() && searchRoot.span().startIndex() == idx) {
      return searchRoot;
    }

    for (final SynNode child : searchRoot) {
      if (idx >= child.span().startIndex() && idx <= child.span().endIndex()) {
        return nodeForToken(idx, child);
      }
    }

    throw new SerifException(String.format(
        "Couldn't find a SynNode for token index %d. Perhaps you are looking in the wrong sentence?",
        idx));
  }

  public final Optional<SynNode> lookupByGornAddress(final GornAddress gornAddress) {
    if (root().isPresent()) {
      if (gornAddress.size() == 1) {
        return root();
      } else {
        SynNode curNode = root().get();
        for (final Integer child : gornAddress.subList(1, gornAddress.size())) {
          curNode = curNode.child(child);
        }
        return Optional.of(curNode);
      }
    } else {
      return Optional.absent();
    }
  }

  public WordAndPOS toWordAndPOS(Token tok) {
    final SynNode node = nodeForToken(tok);

    return WordAndPOS.fromWordThenPOS(node.headWord(), node.headPreterminal().tag());
  }

  public void preorderTraversal(SynNode.PreorderVisitor visitor) {
    if (root().isPresent()) {
      root().get().preorderTraversal(visitor);
    }
  }

  private static void findPreterminalNodes(SynNode node, ImmutableList.Builder<SynNode> preterminals) {
    if (node.isPreterminal()) {
      preterminals.add(node);
    } else {
      for (final SynNode kid : node.children()) {
        findPreterminalNodes(kid, preterminals);
      }
    }
  }
}
