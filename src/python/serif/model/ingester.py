import re

from abc import ABC
from abc import abstractmethod


class Ingester(ABC): 
    def __init__(self, **kwargs):
        self.docid_to_dct = None  # map docids to document creation time (start, end) tuples
        if "metadata_file_path" in kwargs:
            self.docid_to_dct = Ingester.extract_docid_to_dct_from_metadata_file(kwargs["metadata_file_path"])
        if "mtdp_to_serifxml_preprocessing_metadata_file_path" in kwargs:
            self.docid_to_dct = Ingester.extract_docid_to_dct_from_mtdp_metadata_file(
                    kwargs["mtdp_to_serifxml_preprocessing_metadata_file_path"])

    @abstractmethod
    def ingest(self, filepath):
        pass

    @staticmethod
    def extract_docid_to_dct_from_metadata_file(metadata_file_path):
        """
        :param metadata_file_path:
        :return: dictionary mapping document ids to document creation time (start, end) tuples
        """
        lines = [l.strip().split('\t') for l in open(metadata_file_path, 'r').readlines()]
        docid_to_dct = {l[0]: Ingester.normalize_doc_creation_time(l[2]) for l in lines}
        return docid_to_dct

    @staticmethod
    def extract_docid_to_dct_from_mtdp_metadata_file(mtdp_metadata_file):
        """
        :param mtdp_metadata_file:
        :return: dictionary mapping document ids to document creation time (start, end) tuples
        """

        lines = [l.strip().split('\t') for l in open(mtdp_metadata_file, 'r').readlines()]
        docid_to_dct_filepath = {l[0]: l[1] for l in lines}

        docid_to_dct = dict()
        for docid, dct_filepath in docid_to_dct_filepath.items():

            with open(dct_filepath, "r") as f:
                metadata_for_docid = [l.strip() for l in f.readlines()]

            assert docid == metadata_for_docid[0]

            dct_start = metadata_for_docid[1]
            dct_end = metadata_for_docid[2]

            docid_to_dct[docid] = (dct_start, dct_end)

        return docid_to_dct

    @staticmethod
    def normalize_doc_creation_time(dct="20200405"):
        """
        :param dct: string representing document creation time, with format YYYYMMDD
        :return: normalized start, end time tuple
        """
        m = re.fullmatch(pattern=r"([12]\d\d\d)(\d\d)(\d\d)", string=dct)
        start = "{}-{}-{}T00:00:00".format(m.group(1), m.group(2), m.group(3))
        end = "{}-{}-{}T23:59:59".format(m.group(1), m.group(2), m.group(3))
        return start, end
