from serif.model.tokenizer_model import TokenizerModel


class WhiteSpaceTokenizerWithExternalStartEndInfo(TokenizerModel):

    def __init__(self, mapping_file, **kwargs):
        super(WhiteSpaceTokenizerWithExternalStartEndInfo, self).__init__(**kwargs)

        # doc_id: {sent_idx: [(s1,e1),(s2,e2),...]}
        self.doc_to_spans = self.load_start_end_char_annotations(mapping_file)

        self.external_tag_file = True  # to permit the model to accept annotations file as argument

    def load_start_end_char_annotations(self, mapping_file):
        with open(mapping_file, "r") as mf:
            tok_2_ann = {line.strip().split()[0]: line.strip().split()[1] for line in mf.readlines()}
        print(tok_2_ann)
        source_doc_2_span_info = {}
        for tok, ann in tok_2_ann.items():
            source_doc_id = tok.split("/")[-1]
            with open(tok, "r") as tf:
                sents_text = [l.strip() for l in tf.readlines()]
            with open(ann, "r") as af:
                sents_anno = [[(anno.split("_")[0], anno.split("_")[1]) for anno in l.strip().split()] for l in
                              af.readlines()]
            sent_idx_2_anno = {}
            for i, (tok_text, anno) in enumerate(zip(sents_text, sents_anno)):
                sent_idx_2_anno[i] = (tok_text, anno)
            source_doc_2_span_info[source_doc_id] = sent_idx_2_anno
        print(source_doc_2_span_info)
        return source_doc_2_span_info

    def add_tokens_to_sentence(self, sentence):
        sentence_idx = sentence.sent_no
        ret = []
        (tok_text, sent_anno) = self.doc_to_spans[sentence.document.docid[:-4] + ".tok"][
            sentence_idx]  # replace .raw with .tok to access mapping_file_for_tokenization
        tokens = tok_text.strip().split()
        assert len(sent_anno) == len(tokens)

        for t, (s, e) in zip(tokens, sent_anno):
            ret.extend(TokenizerModel.add_new_token(sentence.token_sequence, t, int(s), int(e)))

        return ret
