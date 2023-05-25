import json
import os

from serif.model.document_model import DocumentModel


class BETTERBertIRInfoCollector(DocumentModel):
    def __init__(self, **kwargs):
        super(BETTERBertIRInfoCollector, self).__init__(**kwargs)
        self.output_path = os.path.join(self.argparse.output_directory, "corpus_info.ljson")
        with open(self.output_path, 'w') as fp:
            pass

    def process_document(self, serif_doc):
        presumed_output_path = os.path.join(self.argparse.output_directory, "{}.xml".format(serif_doc.docid))
        for sentence in serif_doc.sentences or ():
            sent_no = sentence.sent_no
            num_ems_by_type = dict()
            for event_mention in sentence.event_mention_set or ():
                num_ems_by_type[event_mention.event_type] = num_ems_by_type.get(event_mention.event_type, 0) + 1
            with open(self.output_path, 'a') as wfp:
                wfp.write("{}\n".format(json.dumps(
                    {"doc_id": serif_doc.docid, "sent_id": sent_no, "serif_path": presumed_output_path,
                     "event_mention_statistics": num_ems_by_type}, ensure_ascii=False)))
