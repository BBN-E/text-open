import re,logging

from serif import Document
from serif.model.ingester import Ingester
from serif.io.bpjson.reader import Corpus

logger = logging.getLogger(__name__)

class BpjsonIngester(Ingester):
    def __init__(self, lang, **kwargs):
        super(BpjsonIngester, self).__init__(**kwargs)
        self.lang = lang

    def ingest(self, filepath):
        documents = []
        corpus = Corpus.from_file(filepath)
        control_characters_re = re.compile(r'([{}])'.format("".join({"\u202C", "\u202B"})))
        for segment in corpus.segments:
            if "Sentence" not in segment.segment_sections:
                logger.info("Skipping segment: {} because we don't see any sentences".format(segment.entry_id))
                continue
            serif_doc = Document.from_string(control_characters_re.sub(" ",segment.text), self.lang, segment.entry_id)
            #print(segment.entry_id)
            # Load and create sentences
            sentences = serif_doc.add_new_sentences()
            region = serif_doc.regions[0]
            all_segment_sections = []
            all_sse = [e for e in segment.segment_sections["Sentence"].entries]
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
                        #print("Removing based on: " + str(sse1[0]) + " " + str(sse1[1]) + " - " +  str(sse2[0]) + " " +  str(sse2[1]))
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
                    if sse1[0] >= sse2[0] and sse1[0] <= sse2[1]-1 and sse1[1]-1 >= sse2[1]-1:
                        logger.error("Overlapping sentences, but one isn't contained in the other! {} {} - {} {}"
                                       .format(sse1[0], sse1[1], sse2[0], sse2[1]))

            all_sse.sort(key = lambda x: x[0])
            for segment_section_entry in all_sse:
                sentences.add_new_sentence(
                    start_char=segment_section_entry[0],
                    end_char=segment_section_entry[1],
                    region=region
                )
            documents.append(serif_doc)
        return documents
