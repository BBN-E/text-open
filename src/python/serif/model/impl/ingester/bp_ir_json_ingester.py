import json
import logging
import re

from serif import Document
from serif.model.ingester import Ingester

logger = logging.getLogger(__name__)


def add_sentences(serif_doc, sentence_segment):
    if len(sentence_segment) < 1:
        return
    # Load and create sentences
    sentences = serif_doc.add_new_sentences()
    region = serif_doc.regions[0]
    all_segment_sections = []
    all_sse = [e for e in sentence_segment]
    sses_to_remove = set()

    # Remove segment section entries that are entirely contained in some other one
    for sse1 in all_sse:
        for sse2 in all_sse:
            if sse1 == sse2:
                continue
            if sse1[0] >= sse2[0] and sse1[1] <= sse2[1]:
                if sse1[0] == sse2[0] and sse1[1] == sse2[1] and all_sse.index(sse1) < all_sse.index(sse2):
                    # Keep earlier one if offsets match exactly
                    continue
                # print("Removing based on: " + str(sse1[0]) + " " + str(sse1[1]) + " - " +  str(sse2[0]) + " " +  str(sse2[1]))
                sses_to_remove.add(sse1)
                continue

    for sse in sses_to_remove:
        all_sse.remove(sse)

    for sse1 in all_sse:
        for sse2 in all_sse:
            if sse1 == sse2:
                continue
            # Sanity check for overlap, but when not entirely contained in the other
            # We subtract one from the ends because the end offset is not inclusive
            if sse1[0] >= sse2[0] and sse1[0] <= sse2[1] - 1 and sse1[1] - 1 >= sse2[1] - 1:
                logger.error("Overlapping sentences, but one isn't contained in the other! {} {} - {} {}"
                             .format(sse1[0], sse1[1], sse2[0], sse2[1]))

    all_sse.sort(key=lambda x: x[0])
    for segment_section_entry in all_sse:
        sentences.add_new_sentence(
            start_char=segment_section_entry[0],
            end_char=segment_section_entry[1],
            region=region
        )


def correct_control_characters(original_text):
    string = original_text.replace("\\r\\n", " \r\n ").replace("\\r", "\r ").replace("\\n", "\n ").replace("\\t", "\t ")
    string = re.sub(r'\\x\w\w', '    ', string)
    string = re.sub(r'\\\w([^\w])', '\1  ', string)
    string = re.sub(r'\\u[abcdef\d][abcdef\d][abcdef\d][abcdef\d]', '      ', string)
    string = re.sub(r"\\'", " '", string)
    return string


class BpIRjsonIngester(Ingester):
    date_re = re.compile("(\d\d\d\d)/(\d\d)/(\d\d)")

    def __init__(self, lang, **kwargs):
        super(BpIRjsonIngester, self).__init__(**kwargs)
        self.lang = lang
        self.use_provided_sentence_segmentation = True
        if "use_provided_sentence_segmentation" in kwargs and kwargs["use_provided_sentence_segmentation"].lower() == "false":
            self.use_provided_sentence_segmentation = False

    def ingest(self, filepath):
        documents = []
        control_characters_re = re.compile(r'([{}])'.format("".join({"\u202C", "\u202B"})))
        count = 0
        with open(filepath, 'r', encoding='utf8') as f:
            while True:
                count += 1

                line = f.readline()

                if not line:
                    break

                data = json.loads(line)

                full_text = data["derived-metadata"]["text"]
                full_text = control_characters_re.sub(" ", full_text)
                full_text = correct_control_characters(full_text)
                # domain = data["derived-metadata"]["domain"]
                guessed_publish_date = data["derived-metadata"]["guess-publish-date"]
                # crawl_date = data["derived-metadata"]["crawl-date"]
                doc_id = data["derived-metadata"]["id"]
                language = data["derived-metadata"]["language"]
                # lang_detect_confidence = data["derived-metadata"]["langdetect-confidence"]

                serif_doc = Document.from_string(full_text, language, doc_id)
                m = BpIRjsonIngester.date_re.match(guessed_publish_date)
                if m is not None:
                    year = m.group(1)
                    month = m.group(2)
                    day = m.group(3)
                    date_string = year + "-" + month + "-" + day
                    serif_doc.document_time_start = date_string + "T23:59:59"
                    serif_doc.document_time_end = date_string + "T00:00:00"

                if self.use_provided_sentence_segmentation is True:
                    sentence_breaking_segments = list()
                    for segment in data["derived-metadata"].get("segment-sections", list()):
                        if segment['structural-element'] == "Sentence":
                            sentence_breaking_segments.append([segment['start'], segment['end']])
                    add_sentences(serif_doc, sentence_breaking_segments)
                documents.append(serif_doc)
        return documents


if __name__ == "__main__":
    ins = BpIRjsonIngester("arabic")
    input_path = "/d4m/better/data/ir_docker_full_121720.auto.fast/corpus/turkey-run-arabic-corpus-segmented.jl"
    ins.ingest(input_path)
