import argparse
import json
import bisect
from itertools import chain

__FORMAT_TYPE__ = "bp-corpus"
__FORMAT_VERSION__ = "v8f"

IGNORE_QUAD_CLASS = True	# applicable for Abstract events

class Corpus:
    def __init__(self,data):
        self.__corpus_id = data.get('corpus-id', '')
        self.__format_type = data.get('format-type', '')
        self.__format_version = data.get('format-version', '')
        self.__provenance = data.get('provenance', '')
        # assert(self.__format_type == __FORMAT_TYPE__)
        # assert(self.__format_version == __FORMAT_VERSION__)
        # should be a list of supported types
        self.__segments = []
        for entry_id, entry_value in data['entries'].items():
            assert(entry_id == entry_value['entry-id'])
            self.add_entry(entry_value)

    @staticmethod
    def from_file(filepath):
        with open(filepath, 'r', encoding='utf8') as f:
            data = json.load(f)
        return Corpus(data)

    def add_entry(self, entry_dict):
        if entry_dict['segment-type'] == 'sentence':
            segment = Segment(doc_id=entry_dict['doc-id'], entry_dict=entry_dict)
            segment.add_segment_section_entry("Sentence", 0, len(segment.text) - 1)
        elif entry_dict['segment-type'] in {'document','highlight'}:
            segment = Segment(doc_id=entry_dict['doc-id'], entry_dict=entry_dict)
        else:
            raise RuntimeError(
                'segment-type: {} not implemented!'.format(
                    entry_dict['segment-type']
                )
            )

        self.__segments.append(segment)


    @property
    def format_type(self):
        return self.__format_type

    @property
    def format_version(self):
        return self.__format_version

    @property
    def segments(self):
        return self.__segments

    def clear_annotation(self):
        for segment in self.segments:
            segment.clear_annotation()

    def save(self, output_file):
        entries = {}
        for segment in self.segments:
            corpus_entries = segment.to_json_dict()
            entry_id = corpus_entries['entry-id']
            assert(entry_id not in entries)
            entries[entry_id] = corpus_entries
        data = {
            'corpus-id': self.__corpus_id,
            'entries': entries,
            'format-type': self.__format_type,
            'format-version': self.__format_version,
            'provenance': self.__provenance
        }
        with open(output_file, 'w', encoding='utf-8') as output:
            json.dump(
                data, output, ensure_ascii=False, indent=2, sort_keys=True)


class SegmentSection:
    def __init__(self, structural_element):
        self.__structural_element = structural_element
        self.__entries = []

    def add_entry(self, start, end):
        bisect.insort(self.__entries, (start, end))

    @property
    def entries(self):
        return self.__entries


class Segment:
    def __init__(self, *, doc_id, entry_dict):
        self.__abstract_events = dict()
        self.__basic_events = dict()
        self.__granular_templates = dict()

        self.__span_sets = dict()
        self.__relations = dict()
        self.__coref_events = []
        self.__doc_id = doc_id
        self.__text = entry_dict['segment-text']
        self.__entry_id = entry_dict.get('entry-id', "")
        self.__sent_id = entry_dict.get('sent-id', "")
        self.__segment_sections = dict()

        if "segment-sections" in entry_dict:
            segment_sections = entry_dict.get("segment-sections", [])
            for segment_section_entry in segment_sections:
                structural_element = segment_section_entry["structural-element"]
                segment_section_obj = self.__segment_sections.get(
                    structural_element,
                    SegmentSection(structural_element)
                )
                segment_section_obj.add_entry(
                    segment_section_entry['start'],
                    segment_section_entry['end']
                )
                self.__segment_sections[structural_element] = segment_section_obj

        # read abstract-events
        if "abstract-events" in entry_dict.get('annotation-sets', {}):
            events = entry_dict.get('annotation-sets', {}).get('abstract-events', {})
            events_data = events.get('events', {})
            spans_sets_data = events.get('span-sets', {})
            for span_set_name, span_set_value in spans_sets_data.items():
                spans = []
                for span_data in span_set_value.get('spans', []):
                    # Data is often missing from span, use defaults when missing
                    hstring = None
                    string = None
                    start = -1
                    end = -1
                    hinferred = None
                    hstart = -1
                    hend = -1
                    synclass = None

                    if 'hstring' in span_data:
                        hstring = span_data['hstring']
                    if 'string' in span_data:
                        string = span_data['string']
                    if 'start' in span_data:
                        start = span_data['start']
                    if 'end' in span_data:
                        end = span_data['end']
                    if 'hinferred' in span_data:
                        hinferred = span_data['hinferred']
                    if 'hstart' in span_data:
                        hstart = span_data['hstart']
                    if 'hend' in span_data:
                        hend = span_data['hend']
                    if 'synclass' in span_data:
                        synclass = span_data['synclass']
                                           
                    spans.append(Span(hstring=hstring, string=string, 
                                      start=start, end=end, 
                                      hinferred=hinferred, hstart=hstart, 
                                      hend=hend, synclass=synclass))

                self.__span_sets[span_set_name] = SpanSet(
                    span_set_name=span_set_name,
                    spans=spans
                )
            for event_name, event_dict in events_data.items():
                agents = []
                patients = []
                for agent_span_set_id in event_dict['agents']:
                    agents.append(self.span_sets[agent_span_set_id])
                for patient_span_set_id in event_dict['patients']:
                    patients.append(self.span_sets[patient_span_set_id])

                if IGNORE_QUAD_CLASS is True:
                    helpful_harmful_value = 'neutral'
                    material_verbal_value = 'material'
                else:
                    helpful_harmful_value = event_dict['helpful-harmful']
                    material_verbal_value = event_dict['material-verbal']

                abstract_event = AbstractEvent(
                    event_id=event_dict['eventid'],
                    helpful_harmful=helpful_harmful_value,
                    material_verbal=material_verbal_value,
                    anchor_span_set=self.span_sets[event_dict['anchors']],
                    agent_span_sets=agents,
                    patient_span_sets=patients,
                    agent_offsets=event_dict.get('agents_offsets', {}),
                    patient_offsets=event_dict.get('patients_offsets', {}),
                    anchor_offsets=event_dict.get('anchor_offsets', {}),
                    ref_events=event_dict.get('ref-events', [])
                )
                self.add_abstract_event(abstract_event)

        # read basic-events
        if "basic-events" in entry_dict.get('annotation-sets', {}):
            events = entry_dict.get('annotation-sets', {}).get('basic-events', {})
            events_data = events.get('events', {})
            spans_sets_data = events.get('span-sets', {})
            relations_data = events.get('includes-relations', {})
            coref_events_data = events.get('template-filler-coref-events', [])  # list of (list of event-ids)
            for span_set_name, span_set_value in spans_sets_data.items():
                spans = []
                for span_data in span_set_value.get('spans', []):
                    if 'hstring' in span_data:
                        spans.append(
                            Span(
                                hstring=span_data['hstring'],
                                hinferred=span_data.get('hinferred', None),
                                string=span_data['string'],
                                start=span_data['start'],
                                end=span_data['end'],
                                hstart=span_data['hstart'],
                                hend=span_data['hend'],
                                synclass=span_data.get('synclass', None)
                            )
                        )
                    else:
                        spans.append(
                            Span(
                                hstring=None,
                                string=span_data['string'],
                                start=span_data['start'],
                                end=span_data['end'],
                                synclass=span_data.get('synclass', None)
                            )
                        )
                self.__span_sets[span_set_name] = SpanSet(
                    span_set_name=span_set_name,
                    spans=spans
                )

            # cache span set IDs for events
            event_to_span_set = dict()
            for event_name, event_dict in events_data.items():
                event_id = event_dict['eventid']
                anchor_span_set = self.span_sets[event_dict['anchors']]
                event_to_span_set[event_id] = anchor_span_set

            for event_name, event_dict in events_data.items():
                agents = []
                patients = []
                money = []
                arg = []
                for agent_span_set_id in event_dict['agents']:
                    agents.append(self.span_sets[agent_span_set_id])
                for patient_span_set_id in event_dict['patients']:
                    patients.append(self.span_sets[patient_span_set_id])
                for ref_event_id in event_dict['ref-events']:
                    arg.append(event_to_span_set[ref_event_id])
                soa = False
                if 'state-of-affairs' in event_dict:
                    soa = event_dict['state-of-affairs']
                if 'money' in event_dict:
                    for money_span_set_id in event_dict['money']:
                        money.append(self.span_sets[money_span_set_id])

                basic_event = BasicEvent(
                    event_id=event_dict['eventid'],
                    event_type=event_dict['event-type'],
                    anchor_span_set=self.span_sets[event_dict['anchors']],
                    agent_span_sets=agents,
                    patient_span_sets=patients,
                    agent_offsets=event_dict.get('agents_offsets', {}),
                    patient_offsets=event_dict.get('patients_offsets', {}),
                    anchor_offsets=event_dict.get('anchor_offsets', {}),
                    ref_events=event_dict.get('ref-events', []),
                    ref_event_span_sets=arg,
                    state_of_affairs=soa,
                    money_span_sets=money,
                    money_offsets=event_dict.get('money_offsets', {}),
                )
                self.add_basic_event(basic_event)

            for ssid, ssids_related in relations_data.items():
                self.__relations[ssid] = []
                for entry in ssids_related:
                    self.__relations[ssid].append(
                        self.__span_sets[entry]
                    )

            for coref_events in coref_events_data:
                # coref_anchor_span_sets = []
                # for event_id in coref_events:
                #     assert event_id in event_to_span_set
                #     # event_to_span_set[event_id] gets me the anchor span_set for the event_id
                #     coref_anchor_span_sets.append(event_to_span_set[event_id])
                # self.__coref_events.append(coref_anchor_span_sets)
                self.__coref_events.append(coref_events)

            # read granular-templates
            if "granular-templates" in events:
                templates_data = events.get('granular-templates', {})

                for template_name, template_dict in templates_data.items():
                    template_anchor_span_set = None
                    template_id = None
                    template_type = None
                    type_ = None
                    completion = None
                    over_time = None
                    coordinated = None
                    project_type = None
                    role_to_args = dict()

                    for arg_role, arg_dict_set in template_dict.items():
                        if arg_role == "template-anchor":
                            template_anchor_span_set = self.span_sets[arg_dict_set]
                        elif arg_role == "template-id":
                            template_id = arg_dict_set
                        elif arg_role == "template-type":
                            template_type = arg_dict_set
                        elif arg_role == "type":
                            type_ = arg_dict_set
                        elif arg_role == "completion":
                            completion = arg_dict_set
                        elif arg_role == "over-time":
                            over_time = arg_dict_set
                        elif arg_role == "coordinated":
                            coordinated = arg_dict_set
                        elif arg_role == "project-type":
                            project_type = arg_dict_set
                        else:
                            # if arg_role not in role_to_args:
                            #     role_to_args[arg_role] = []

                            args = []
                            #print(arg_role)
                            #
                            for arg_dict in arg_dict_set:
                                arg = []
                                # if it's event ID
                                if "event-id" in arg_dict:
                                    ref_event_id = arg_dict["event-id"]

                                    arg = {
                                        'span_set': event_to_span_set[ref_event_id],
                                        'event': self.basic_events[ref_event_id]
                                    }
                                    for key, value in arg_dict.items():
                                        if key != "event-id":
                                            arg[key] = value

                                # if it's span set ID
                                elif "ssid" in arg_dict:
                                    arg = {
                                        'span_set': self.span_sets[arg_dict["ssid"]]
                                    }
                                    for key, value in arg_dict.items():
                                        if key != "ssid":
                                            arg[key] = value

                                args.append(arg)
                            role_to_args[arg_role] = args

                    granular_template = GranularTemplate(
                        event_id=template_id,
                        event_type=template_type,
                        type_=type_,
                        completion=completion,
                        over_time=over_time,
                        coordinated=coordinated,
                        project_type=project_type,
                        anchor_span_set=template_anchor_span_set,
                        role_to_args=role_to_args
                    )
                    self.add_granular_template(granular_template)

    def add_segment_section_entry(self, structural_element, start, end):
        segment_section_obj = self.__segment_sections.get(
            structural_element,
            SegmentSection(structural_element)
        )
        segment_section_obj.add_entry(
            start,
            end
        )
        self.__segment_sections[structural_element] = segment_section_obj

    @property
    def abstract_events(self):
        return self.__abstract_events

    @property
    def basic_events(self):
        return self.__basic_events

    @property
    def granular_templates(self):
        return self.__granular_templates

    @property
    def segment_sections(self):
        return self.__segment_sections

    @property
    def span_sets(self):
        return self.__span_sets

    @property
    def text(self):
        return self.__text

    @property
    def entry_id(self):
        return self.__entry_id

    @property
    def sent_id(self):
        return self.__sent_id

    @property
    def doc_id(self):
        return self.__doc_id

    @property
    def relations(self):
        return self.__relations

    @property
    def coref_events(self):
        return self.__coref_events

    # Creates a span set and returns the span set id.  If an identical span set
    # already existed, that span set id is returned instead of creating a new
    # one.
    def add_span_set(self, *, span_strings):
        spans = []
        for span_string in span_strings:
            assert(span_string in self.text)
            spans.append(Span(hstring=None, string=span_string))
        for ss_id, span_set in self.span_sets.items():
            if spans == span_set.spans:
                return ss_id
        new_ss_id = 'ss-' + str(len(self.span_sets) + 1)
        self.span_sets[new_ss_id] = SpanSet(span_set_name=new_ss_id,
                                            spans=spans)
        return new_ss_id

    # Add a new abstract event that references span sets that already exist on
    # this object
    def add_abstract_event(self, abstract_event):
        # We have to cast to string because MITRE was mixing strings and ints
        key = str(abstract_event.event_id)
        assert(key not in self.abstract_events)
        self.__abstract_events[key] = abstract_event

    def add_basic_event(self, basic_event):
        # We have to cast to string because MITRE was mixing strings and ints
        key = str(basic_event.event_id)
        assert(key not in self.basic_events)
        self.__basic_events[key] = basic_event

    def add_granular_template(self, granular_template):
        # We have to cast to string because MITRE was mixing strings and ints
        key = str(granular_template.event_id)
        assert(key not in self.granular_templates)
        self.__granular_templates[key] = granular_template

    def clear_annotation(self):
        self.abstract_events.clear()
        self.span_sets.clear()

    def to_json_dict(self):
        events = {}
        span_sets = {}
        for event_id, event in self.abstract_events.items():
            events[event_id] = event.to_json_dict()
        for ss_id, span_set in self.span_sets.items():
            span_sets[ss_id] = span_set.to_json_dict()
        abstract_events = {
            'events': events,
            'span-sets': span_sets
        }
        annotation_sets = {
            'abstract-events': abstract_events
        }
        data = {
            'annotation-sets': annotation_sets,
            'doc-id': self.__doc_id,
            'entry-id': self.entry_id,
            'segment-text': self.text,
            'segment-type': 'sentence',
            'sent-id': str(self.sent_id)
        }
        return data


class Event:
    def __init__(self, *, event_id, anchor_span_set, anchor_offsets=None):
        self.__event_id = event_id
        self.__anchors = anchor_span_set
        self.__anchor_offsets = anchor_offsets

    @property
    def anchors(self):
        return self.__anchors

    @property
    def anchor_offsets(self):
        return self.__anchor_offsets

    @property
    def event_id(self):
        return self.__event_id


class AbstractEvent(Event):
    # Removed SPECIFIED and NOT as they no longer show up as of 8d
    HELPFUL_HARMFUL_TYPES = {'helpful', 'harmful', 'neutral', 'unk'}
    MATERIAL_VERBAL_TYPES = {'material', 'verbal', 'both', 'unk'}

    def __init__(self, *, event_id, helpful_harmful, material_verbal,
                 anchor_span_set, agent_span_sets, patient_span_sets,
                 anchor_offsets=None, agent_offsets=None, patient_offsets=None, ref_events=()):
        super().__init__(event_id=event_id, anchor_span_set=anchor_span_set,
                         anchor_offsets=anchor_offsets)

        if helpful_harmful not in self.HELPFUL_HARMFUL_TYPES:
            raise RuntimeError(
                'Unexpected helpful-harmful value: ' + helpful_harmful)
        if material_verbal not in self.MATERIAL_VERBAL_TYPES:
            raise RuntimeError(
                'Unexpected material-verbal value: ' + material_verbal)
        self.__helpful_harmful = helpful_harmful
        self.__material_verbal = material_verbal
        self.__agents = agent_span_sets
        self.__patients = patient_span_sets
        self.__ref_events = ref_events
        self.__agent_offsets = agent_offsets
        self.__patient_offsets = patient_offsets

    @property
    def agents(self):
        return self.__agents

    @property
    def agent_offsets(self):
        return self.__agent_offsets

    @property
    def patients(self):
        return self.__patients

    @property
    def patient_offsets(self):
        return self.__patient_offsets

    @property
    def ref_events(self):
        return self.__ref_events

    def add_ref_event_id(self, event_id):
        self.__ref_events.append(event_id)

    @property
    def helpful_harmful(self):
        return self.__helpful_harmful

    @property
    def material_verbal(self):
        return self.__material_verbal

    def to_json_dict(self):
        data = {
            'agents': sorted([x.ss_id for x in self.agents]),
            'anchors': self.anchors.ss_id,
            'eventid': self.event_id,
            'helpful-harmful': self.helpful_harmful,
            'material-verbal': self.material_verbal,
            'patients': sorted([x.ss_id for x in self.patients]),
            'ref-events': sorted(self.ref_events),
            'anchor_offsets': self.anchor_offsets,
            'agent_offsets': self.__agent_offsets,
            'patient_offsets': self.__patient_offsets
        }
        return data


class BasicEvent(Event):

    def __init__(self, *, event_id, event_type,
                 anchor_span_set, agent_span_sets, patient_span_sets,
                 anchor_offsets=None, agent_offsets=None, patient_offsets=None,
                 ref_events=None,
                 ref_event_span_sets=None,
                 state_of_affairs=None,
                 money_span_sets=None, money_offsets=None,):
        super().__init__(event_id=event_id, anchor_span_set=anchor_span_set, anchor_offsets=anchor_offsets)
        self.__event_type = event_type
        self.__agent_span_sets = agent_span_sets
        self.__patient_span_sets = patient_span_sets
        self.__agent_offsets = agent_offsets
        self.__patient_offsets = patient_offsets
        self.__ref_events = ref_events
        if self.__ref_events is None:
            self.__ref_events = []
        self.__ref_event_span_sets = ref_event_span_sets
        self.__state_of_affairs = state_of_affairs
        self.__money_span_sets = money_span_sets
        self.__money_offsets = money_offsets
    @property
    def agent_span_sets(self):
        return self.__agent_span_sets

    @property
    def patient_span_sets(self):
        return self.__patient_span_sets

    @property
    def agent_offsets(self):
        return self.__agent_offsets

    @property
    def patient_offsets(self):
        return self.__patient_offsets

    @property
    def event_type(self):
        return self.__event_type

    @property
    def ref_event_span_sets(self):
        return self.__ref_event_span_sets

    @property
    def ref_events(self):
        return self.__ref_events

    @property
    def state_of_affairs(self):
        return self.__state_of_affairs

    @property
    def money_span_sets(self):
        return self.__money_span_sets

    @property
    def money_offsets(self):
        return self.__money_offsets


class GranularTemplate(Event):

    def __init__(self, *, event_id, event_type,
                 type_, completion, over_time, coordinated, project_type,
                 anchor_span_set, anchor_offsets=None,
                 role_to_args=None
                 ):
        super().__init__(event_id=event_id, anchor_span_set=anchor_span_set, anchor_offsets=anchor_offsets)

        self.__event_type = event_type
        self.__type = type_
        self.__completion = completion
        self.__over_time = over_time
        self.__coordinated = coordinated
        self.__project_type = project_type

        self.__anchor_span_set = anchor_span_set

        self.__role_to_args = role_to_args

    @property
    def event_type(self):
        return self.__event_type

    @property
    def type_(self):
        return self.__type

    @property
    def completion(self):
        return self.__completion

    @property
    def over_time(self):
        return self.__over_time

    @property
    def coordinated(self):
        return self.__coordinated

    @property
    def project_type(self):
        return self.__project_type

    @property
    def anchor_span_set(self):
        return self.__anchor_span_set

    @property
    def role_to_args(self):
        return self.__role_to_args


class SpanSet:
    def __init__(self, *, span_set_name, spans):
        self.__spans = spans
        self.__ss_id = span_set_name

    @property
    def spans(self):
        return self.__spans

    @property
    def ss_id(self):
        return self.__ss_id

    def to_json_dict(self):
        spans = []
        for span in self.spans:
            if span.hstring is None:
                spans.append({'string': span.string})
            else:
                spans.append({
                    'hstring': span.hstring,
                    'string': span.string
                })
        data = {
            'spans': spans,
            'ssid': self.ss_id
        }
        return data

    def __repr__(self):
        return json.dumps(self.to_json_dict())


class Span:
    def __init__(self, *, hstring, string, start=-1, end=-1, hinferred=None, hstart=-1, hend=-1, synclass=None):
        # hstring is the head of the span, but it is not always provided.  If it
        # was not provided, it will be None
        self.__hstring = hstring
        self.__hinferred = hinferred
        self.__string = string
        self.__start = start
        self.__end = end
        self.__hstart = hstart
        self.__hend = hend
        self.__synclass = synclass

    def __eq__(self, other):
        if isinstance(other, Span):
            return (self.__hstring == other.__hstring
                    and self.__string == other.__string)
        return NotImplemented

    def __hash__(self):
        return hash(
            (self.__hstring, self.__hinferred, 
             self.__string, self.__start, 
             self.__end, self.__hstart, 
             self.__hend, self.__synclass,))

    @property
    def hstring(self):
        return self.__hstring

    @property
    def string(self):
        return self.__string

    @property
    def start(self):
        return self.__start

    @property
    def end(self):
        return self.__end

    @property
    def hinferred(self):
        return self.__hinferred

    @property
    def hstart(self):
        return self.__hstart

    @property
    def hend(self):
        return self.__hend

    @property
    def synclass(self):
        return self.__synclass


def _main(args):
    corpus = Corpus.from_file(args.input_file)
    print('Read {} segments'.format(len(corpus.segments)))
    corpus.save(args.output_file)


def _parser_setup():
    parser = argparse.ArgumentParser(
        description="Test ingestion and serialization of MITRE's JSON format")
    parser.add_argument('input_file', help='Input file')
    parser.add_argument('output_file', help='Output file')
    return parser


def test2():
    f = "/d4m/better/data/auto_ir_dryrun_112320/app/ir-tasks.json"
    with open(f) as fp:
        ir_tasks = json.load(fp)
    docid_to_annotation = dict()
    for task in ir_tasks:
        docid_to_annotation.update(task['task-docs'].items())
        for request in task['requests']:
            docid_to_annotation.update(request['req-docs'].items())
    corpus_obj = Corpus({
        "corpus-id":"From IR",
        "entries":docid_to_annotation,
        "format-type":__FORMAT_TYPE__,
        "format-version":__FORMAT_VERSION__
    })


if __name__ == '__main__':
    _main(_parser_setup().parse_args())
