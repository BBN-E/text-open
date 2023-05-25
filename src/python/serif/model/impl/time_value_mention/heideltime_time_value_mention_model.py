import os,sys,io,re
import xml.etree.ElementTree as ET

import time
#import wrapt_timeout_decorator

from serifxml3 import Document
from serif.model.time_value_mention_model import TimeValueMentionModel

import HeidelTime

from html.parser import HTMLParser


class TimexHTMLParser(HTMLParser):  # courtesy of @dzajic
    def __init__ (self):
        super().__init__()
        self.underlying = ""
        self.inTIMEX3 = False
        self.timex3 = []
        self.timex3_attr = []

    def handle_starttag(self, tag, attrs):
        if tag == "timex3":
            self.inTIMEX3 = True
            self.timex3_attr.append({k:v for (k,v) in attrs})

    def handle_endtag(self, tag):
        if tag == "timex3":
            self.inTIMEX3 = False

    def handle_data(self, data):
        if self.inTIMEX3:
            self.timex3.append((data, len(self.underlying), len(self.underlying + data) - 1))
        self.underlying += data


class HeidelTime_TimeValueMentionModel(TimeValueMentionModel):

    class Date:
        def __init__(self, day, month, year):
            self.day = day
            self.month = month
            self.year = year

    def __init__(self, lang='english', **kwargs):
        super(HeidelTime_TimeValueMentionModel,self).__init__(**kwargs)
        self.lang = lang
        self.max_tokens = 128
        self.total_time = 0
        self.avg_time_per_sent = 0
        self.num_sents_seen = 0

    def load_model(self):
        self.heideltimewrapper = HeidelTime.HeidelTimeWrapper(self.lang, doc=None, output='timeml')

    def unload_model(self):
        del self.heideltimewrapper
        self.heideltimewrapper = None

    # @wrapt_timeout_decorator.timeout(100, timeout_exception=TimeoutError)
    def parse_heideltime(self, text, date_ref=None):
        return str(self.heideltimewrapper.parse(text, date_ref=date_ref))

    # Overrides TimeValueMentionModel.get_timex_start_end_info
    def add_time_value_mentions_to_sentence(self, sentence):
        added_value_mentions_all = list()
        added_values_all = list()

        # skip longer sentences
        if len(sentence.token_sequence) > self.max_tokens:
            print("sentence exceeds max length - skipping")
            return added_value_mentions_all, added_values_all

        sent_start_end_char_to_token_map = self.build_sent_start_end_char_to_token_map(sentence)
        text = sentence.text
        print(text)

        self.num_sents_seen += 1

        try:
            t1 = time.time()
            dct = sentence.document.document_time_start.split("T")[0] if sentence.document.document_time_start is not None else None
            if dct is not None:
                dct = self.Date(day=dct.split('-')[2], month=dct.split('-')[1], year=dct.split('-')[0])
            heideltime_parse = self.parse_heideltime(text, date_ref=dct)
            t2 = time.time()
            time_for_sent = t2 - t1
            self.total_time += time_for_sent
            print("Running average time to parse sentence = {}".format(self.total_time/self.num_sents_seen))
        except TimeoutError:
            print("timeout error - skipping sentence")
            return added_value_mentions_all, added_values_all

        annotated = re.search("<TimeML>([\s\S]*)<\/TimeML>", heideltime_parse).group(1).strip('\n')
        # produces string of the form '<TIMEX3 tid="t1" type="DATE" value="2021-06-24">Today</TIMEX3> at <TIMEX3 tid="t2" type="TIME" value="2021-06-24T13:32">1:32pm</TIMEX3>, it went on the internet.'

        # print("+++++++++++++++++++++++++++++++++++++++++++++++++")
        # print(annotated)
        # print("+++++++++++++++++++++++++++++++++++++++++++++++++")

        timex_html_parser = TimexHTMLParser()

        timex_html_parser.feed(annotated)
        for i,(timex_text, start_char, end_char) in enumerate(timex_html_parser.timex3):

            timex_attr = timex_html_parser.timex3_attr[i]

            start_token = sent_start_end_char_to_token_map["start_char_to_token"].get(start_char, None)
            end_token = sent_start_end_char_to_token_map["end_char_to_token"].get(end_char, None)

            if start_token is not None and end_token is not None:
                added_value_mentions, added_values = self.add_new_time_value(sentence.value_mention_set,
                                                                             sentence.document.value_set, start_token,
                                                                             end_token, timex_attr['value'])
                added_value_mentions_all.extend(added_value_mentions)
                added_values_all.extend(added_values)

        return added_value_mentions_all, added_values_all


    def build_sent_start_end_char_to_token_map(self, sentence):
        '''builds offset to token map for sentence (0-indexed wrt start of sentence)'''

        sent_start_end_char_to_token_map = {"start_char_to_token": dict(),
                                            "end_char_to_token": dict()}
        for i, token in enumerate(sentence.token_sequence):
            start_char = token.start_char
            end_char = token.end_char
            sent_start_end_char_to_token_map["start_char_to_token"][start_char - sentence.start_char] = token
            sent_start_end_char_to_token_map["end_char_to_token"][end_char - sentence.start_char] = token
        return sent_start_end_char_to_token_map
