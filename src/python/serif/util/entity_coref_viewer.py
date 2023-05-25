import serifxml3
import html

basic_html_template="""
<!DOCTYPE html>
<html>
<head>
<title>Page Title</title>
<style>
.cluster0 {{
background-color:antiquewhite;
}}
.cluster1 {{
background-color:aquamarine;
}}
.cluster2 {{
background-color:cadetblue;
}}
.cluster3 {{
background-color:chartreuse;
}}
.cluster4 {{
background-color:chocolate;
}}
.cluster5 {{
background-color:cornsilk;
}}
.cluster6 {{
background-color:darkorchid;
}}
.cluster7 {{
background-color:darkslateblue;
}}
.cluster8 {{
background-color:dodgerblue;
}}
.cluster9 {{
background-color:gold;
}}
.cluster10 {{
background-color:antiquewhite;
}}
</style>
</head>
<body>

{}

</body>
</html>

"""

def single_document_handler(serif_path):
    serif_doc = serifxml3.Document(serif_path)
    sent_to_token_idx_to_start_marking = dict()
    sent_to_token_idx_to_end_marking = dict()
    assigned_eid = -1
    for eid, entity in enumerate(serif_doc.entity_set or ()):
        if len(entity.mentions) > 1:
            assigned_eid += 1
            for mention in entity.mentions:
                serif_sent = mention.sentence
                sent_no = serif_sent.sent_no
                start_token = mention.tokens[0]
                end_token = mention.tokens[-1]
                sent_to_token_idx_to_start_marking.setdefault(sent_no,dict()).setdefault(start_token.index(),list()).append("<span class=\"cluster{}\">".format(assigned_eid))
                sent_to_token_idx_to_end_marking.setdefault(sent_no, dict()).setdefault(end_token.index(),list()).append("</span>")
    marked_sentences = list()
    for sent_no, sentence in enumerate(serif_doc.sentences):
        marked_tokens = list()
        for token_idx, token in enumerate(sentence.token_sequence):
            c = ""
            for marking in sent_to_token_idx_to_start_marking.get(sent_no,dict()).get(token_idx,()):
                c = c + marking
            c = c + html.escape(token.text.strip())
            for marking in sent_to_token_idx_to_end_marking.get(sent_no,dict()).get(token_idx,()):
                c = c + marking
            marked_tokens.append(c)
        marked_sentences.append(" ".join(marked_tokens))
    return "<h3>{}</h3>\n<br/><p>{}</p><br/>\n".format(serif_doc.docid,"<br/>\n".join(marked_sentences))


def main(input_serifxml_list,output_file):
    doc_paths = list()
    with open(input_serifxml_list) as fp:
        for i in fp:
            i = i.strip()
            doc_paths.append(i)
            if len(doc_paths) > 40:
                break

    res_str = ""

    for doc_path in doc_paths:
        res_str += single_document_handler(doc_path)

    with open(output_file, 'w') as wfp:
        wfp.write("{}\n".format(basic_html_template.format(res_str)))

if __name__ == "__main__":
    input_serifxml_list = "/home/brozonoy/entity_coref.en.1000.list"
    output_path = "/home/hqiu/en.html"
    main(input_serifxml_list,output_path)