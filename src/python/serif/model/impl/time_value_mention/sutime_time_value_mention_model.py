from sutime import SUTime

from serif.model.time_value_mention_model import TimeValueMentionModel


class SUTime_TimeValueMentionModel(TimeValueMentionModel):

    def __init__(self, **kwargs):
        super(SUTime_TimeValueMentionModel, self).__init__(**kwargs)

    def load_model(self):
        self.sutime = SUTime(mark_time_ranges=True, include_range=True)

    def unload_model(self):
        del self.sutime
        self.sutime = None

    # Overrides TimeValueMentionModel.get_timex_start_end_info
    def add_time_value_mentions_to_sentence(self, sentence):

        added_value_mentions_all = list()
        added_values_all = list()

        sent_start_end_char_to_token_map = self.build_sent_start_end_char_to_token_map(sentence)
        text = sentence.text
        sutime_parse_list = self.sutime.parse(text)
        for sutime_parse in sutime_parse_list:

            start_token = sent_start_end_char_to_token_map["start_char_to_token"].get(sutime_parse["start"], None)
            end_token = sent_start_end_char_to_token_map["end_char_to_token"].get(sutime_parse["end"], None)
            if start_token is not None and end_token is not None:
                added_value_mentions, added_values = self.add_new_time_value(sentence.value_mention_set,
                                                                             sentence.document.value_set, start_token,
                                                                             end_token, sutime_parse["timex-value"])
                added_value_mentions_all.extend(added_value_mentions)
                added_values_all.extend(added_values)

        return added_value_mentions_all, added_values_all

    def build_sent_start_end_char_to_token_map(self, sentence):
        sent_start_end_char_to_token_map = {"start_char_to_token": dict(),
                                            "end_char_to_token": dict()}
        for i, token in enumerate(sentence.token_sequence):
            start_char = token.start_char
            end_char = token.end_char
            sent_start_end_char_to_token_map["start_char_to_token"][start_char] = token
            sent_start_end_char_to_token_map["end_char_to_token"][end_char] = token
        return sent_start_end_char_to_token_map
