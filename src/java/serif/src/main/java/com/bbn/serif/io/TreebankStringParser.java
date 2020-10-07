package com.bbn.serif.io;

import com.bbn.bue.common.symbols.Symbol;
import com.bbn.serif.common.SerifException;
import com.bbn.serif.theories.SynNode;
import com.bbn.serif.theories.TokenSequence;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

public final class TreebankStringParser {

  /**
   * @deprecated Prefer {@link #create()}.
   */
  @Deprecated
  public TreebankStringParser() {
  }

  @SuppressWarnings("deprecation")
  public static TreebankStringParser create() {
    return new TreebankStringParser();
  }

  /**
   * A regular expression used by {@link #parseTreebankString} to tokenize the treebank string. This
   * regexp contains four groups: start, headmark, end, token
   */
  static final private Pattern TB_TOKEN_PATTERN =
      Pattern.compile("\\(([^\\s\\^]+)(\\^?)" + "|" +
          "(\\))" + "|" +
          "\\s+" + "|" +
          "([^\\(\\)\\s]+)");

  // Groups used by the regexp.
  private static final int START_GROUP = 1;
  private static final int HEADMARK_GROUP = 2;
  private static final int END_GROUP = 3;
  private static final int TOKEN_GROUP = 4;
  private static final Symbol FAKE_ROOT = Symbol.from("___PARSER_FAKE_ROOT");

  public SynNode parseTreebankString(String tbString, TokenSequence ts) {
    checkArgument(!ts.isEmpty(), "Cannot parse the treebank String of an empty sentence");
    try {
      int token_index = 0;
      final Stack<SynNode.NonterminalBuilder> stack = new Stack<SynNode.NonterminalBuilder>();
      // this is a hack because the SynNodes don't store their headedness
      final Stack<Boolean> headednessStack = new Stack<Boolean>();

      final SynNode.NonterminalBuilder pseudoRoot = SynNode.nonterminalBuilder(FAKE_ROOT);

      stack.push(pseudoRoot);
      headednessStack.push(false);

      Matcher m = TB_TOKEN_PATTERN.matcher(tbString);
      while (m.find()) {
        if (m.group(START_GROUP) != null
            || m.group(TOKEN_GROUP) != null) {
          String tag = m.group(START_GROUP);
          if (tag == null) {
            tag = m.group(TOKEN_GROUP);
          }

          if (m.group(START_GROUP) != null) {
            SynNode.NonterminalBuilder synNode = SynNode.nonterminalBuilder(Symbol.from(tag));
            stack.push(synNode);
            headednessStack.push(m.group(HEADMARK_GROUP).length() > 0);
          } else {
            SynNode.TerminalBuilder synNode = SynNode.terminalBuilder(Symbol.from(tag));
            synNode.tokenIndex(token_index);
            token_index++;
            // parent must be a pre-terminal, so we know this is the head
            stack.peek().appendHead(synNode.build(ts));
          }
        } else if (m.group(END_GROUP) != null) {
          SynNode.Builder completed = stack.pop();
          final boolean isHead = headednessStack.pop();

          if (stack.peek() == pseudoRoot) {
            return completed.build(ts);
          } else {
            if (isHead) {
              stack.peek().appendHead(completed.build(ts));
            } else {
              stack.peek().appendNonHead(completed.build(ts));
            }
          }
        }
      }
    } catch (Exception e) {
      throw new SerifException(String.format(
          "Error parsing treebank string %s", tbString), e);
    }
    throw new SerifException(String.format(
        "Error parsing treebank string %s", tbString));
  }

}
