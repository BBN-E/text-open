from xml.sax.saxutils import escape
import os, sys

current_script_path = __file__
project_root = os.path.realpath(os.path.join(current_script_path, os.path.pardir, os.path.pardir, os.path.pardir))
sys.path.append(project_root)

import serifxml3
import logging


def construct_html_page(html_lang_flag, body_markups):
    return """
<!DOCTYPE html>
<html lang="{}">
  <head>
    <meta charset="utf-8">
    <title>title</title>
  </head>
  <body>
    {}
  </body>
</html>
    """.format(html_lang_flag, body_markups)


def token_highlighter(serif_token_sequences, html_surrounding_start, html_surrounding_end, highlight_spans):
    marked_token = list()
    for idx, serif_token in enumerate(serif_token_sequences):
        current_token_str = escape(serif_token.text)
        for highlight_start, highlight_end in highlight_spans:
            if highlight_start == idx:
                current_token_str = html_surrounding_start + current_token_str
            if highlight_end == idx:
                current_token_str = current_token_str + html_surrounding_end
        marked_token.append(current_token_str)
    return " ".join(marked_token)


def main(html_lang_flag, serifxml_list_path, output_path):
    body_content = list()
    with open(serifxml_list_path) as fp:
        for p in fp:
            p = p.strip()
            serif_doc = serifxml3.Document(p)
            for sentence in serif_doc.sentences:
                token_to_token_idx = dict()
                for idx, token in enumerate(sentence.token_sequence):
                    token_to_token_idx[token] = idx
                for mention in sentence.mention_set or ():
                    tokens = mention.tokens
                    start_token = tokens[0]
                    end_token = tokens[-1]
                    marked_up_sent = token_highlighter(sentence.token_sequence, "<strong>", "</strong>", {
                    (token_to_token_idx[start_token], token_to_token_idx[end_token])})
                    body_content.append("<p>{}</p><p>{}</p>".format(mention.entity_type, marked_up_sent))
    logging.info(len(body_content))
    with open(output_path, 'w') as wfp:
        wfp.write(construct_html_page(html_lang_flag, "\n".join(body_content)))


if __name__ == "__main__":
    import argparse
    parser= argparse.ArgumentParser()
    parser.add_argument("--html_lang_flag",required=True)
    parser.add_argument("--serifxml_list_path",required=True)
    parser.add_argument("--output_path",required=True)
    args = parser.parse_args()
    # html_lang_flag = "ar"
    # serifxml_list_path = "/home/hqiu/tmp/test_ar.list"
    # output_path = "/home/hqiu/tmp/a.html"
    main(args.html_lang_flag, args.serifxml_list_path, args.output_path)
