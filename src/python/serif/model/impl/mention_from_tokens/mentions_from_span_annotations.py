from serif.model.mention_model import MentionModel
import json

multi_word_subtype_to_capitalized = {"apartmentbuilding": "ApartmentBuilding",
                                     "broadcastingcompany": "BroadcastingCompany",
                                     "ChiefOfPolice": "ChiefOfPolice",
                                     "commercialorganization": "CommercialOrganization",
                                     "crimescene": "CrimeScene",
                                     "criminalorganization": "CriminalOrganization",
                                     "geographicpoint": "GeographicPoint",
                                     "governmentarmedforces": "GovernmentArmedForces",
                                     "governmentbuilding": "GovernmentBuilding",
                                     "headofgovernment": "HeadOfGovernment",
                                     "lawenforcementagency": "LawEnforcementAgency",
                                     "legislativebody": "LegislativeBody",
                                     "medicalequipment": "MedicalEquipment",
                                     "medicalpersonnel": "MedicalPersonnel",
                                     "militaryequipment": "MilitaryEquipment",
                                     "militaryinstallation": "MilitaryInstallation",
                                     "militaryofficer": "MilitaryOfficer",
                                     "militaryorganization": "MilitaryOrganization",
                                     "militarypersonnel": "MilitaryPersonnel",
                                     "molotovcocktail": "MolotovCocktail",
                                     "newsagency": "NewsAgency",
                                     "nongovernmentmilitia": "NonGovernmentMilitia",
                                     "numberpercentagevotes": "NumberPercentageVotes",
                                     "officebuilding": "OfficeBuilding",
                                     "organizationofcountries": "OrganizationOfCountries",
                                     "paperballot": "PaperBallot",
                                     "personalidentification": "PersonalIdentification",
                                     "poisongas": "PoisonGas",
                                     "politicalorganization": "PoliticalOrganization",
                                     "professionalposition": "ProfessionalPosition",
                                     "provincestate": "ProvinceState",
                                     "rubberbullets": "RubberBullets",
                                     "sportsfan": "SportsFan",
                                     "symptompresentation": "SymptomPresentation",
                                     "teargas": "TearGas",
                                     "thrownprojectile": "ThrownProjectile",
                                     "topicfiller": "TopicFiller",
                                     "turnoutvoters": "TurnoutVoters",
                                     "urbanarea": "UrbanArea",
                                     "violentcrime": "ViolentCrime",
                                     "votingfacility": "VotingFacility",
                                     "wheeledvehicle": "WheeledVehicle"}


class MentionsFromSpanAnnotations(MentionModel):
    ''' Model for parsing annotated corpus into Serifxml by first reading in entity mention annotations
    '''
    def __init__(self, annotations_file, **kwargs):
        ''':param annotations_file: json of annotated spans per docid'''

        super(MentionsFromSpanAnnotations,self).__init__(**kwargs)

        self.doc_to_spans = self.load_annotations(annotations_file)

        self.external_tag_file = True # to permit the model to accept annotations file as argument

    def load_annotations(self, annotations_file):
        '''
        :param mapping_file: json per-document span annotations file
        see example in /nfs/raid66/u11/users/brozonoy/NER/data/KBP2019-UIUCAnnotation_serifxml/annotations.json
        :return: {"docid": "1-3: {...},
                           "6-14": {...}}
        '''
        with open(annotations_file, "r") as f:
            doc_to_spans = json.load(f)
        return doc_to_spans

    def add_mentions_to_sentence(self, sentence):
        new_mentions = []
        doc_annotations = self.doc_to_spans[sentence.document.docid[:-4]]

        doc_annotations_within_sentence = self.get_doc_spans_within_sentence_offsets(sentence, doc_annotations)
        # print("-----------------------------")
        # print(sentence.id)
        # print([t.text for t in sentence.token_sequence])
        # print(doc_annotations_within_sentence)
        type_start_end = []
        for (start_idx, end_idx) in doc_annotations_within_sentence.keys():
            # print()
            type_string = doc_annotations_within_sentence[start_idx, end_idx]["type"]
            type_list = type_string.split(".")
            assert len(type_list) in {1,2,3}
            if len(type_list) == 3:
                type = type_list[0].upper()
                if type_list[1].lower() in multi_word_subtype_to_capitalized:
                    subtype = multi_word_subtype_to_capitalized[type_list[1].lower()]
                else:
                    subtype = type_list[1].capitalize()
                if type_list[2].lower() in multi_word_subtype_to_capitalized:
                    subsubtype = multi_word_subtype_to_capitalized[type_list[2].lower()]
                else:
                    subsubtype = type_list[2].capitalize()
                type_string = ".".join([type, subtype, subsubtype])

            elif len(type_list) == 2:
                type = type_list[0].upper()
                if type_list[1].lower() in multi_word_subtype_to_capitalized:
                    subtype = multi_word_subtype_to_capitalized[type_list[1].lower()]
                else:
                    subtype = type_list[1].capitalize()
                type_string = ".".join([type, subtype])

            elif len(type_list) == 1:
                type = type_list[0].upper()
                type_string = type

            # print(type_string)

            start_token = self.get_token_with_start_idx(sentence, start_idx)
            # print("start_token", start_token.text)
            end_token = self.get_token_with_end_idx(sentence, end_idx)
            # print("end_token", end_token.text)
            if start_token and end_token:
                new_mentions.extend(self.add_or_update_mention(sentence.mention_set, type_string, "UNDET",
                                                               start_token, end_token, loose_synnode_constraint=True))

        return new_mentions

    def get_doc_spans_within_sentence_offsets(self, sentence, doc_annotations):

        doc_annotations_within_sentence = dict()

        for span_string in doc_annotations.keys():
            span_start, span_end = int(span_string.split("-")[0]), int(span_string.split("-")[1])
            if sentence.token_sequence[0].start_char <= span_start <= span_end <= sentence.token_sequence[-1].end_char:
                doc_annotations_within_sentence[(span_start, span_end)] = doc_annotations[span_string]

        return doc_annotations_within_sentence

    def get_token_with_start_idx(self, sentence, start_idx):
        for t in sentence.token_sequence:
            if t.start_char <= start_idx <= t.end_char:
                return t
        return None

    def get_token_with_end_idx(self, sentence, end_idx):
        # print()
        # print("end_idx", end_idx)
        for t in reversed(sentence.token_sequence):
            # print(t.text, t.end_char)
            if t.start_char <= end_idx <= t.end_char:
                return t
        return None
