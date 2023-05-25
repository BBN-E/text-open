import os,importlib,logging,sys,copy
from serif.model.document_model import DocumentModel


logger = logging.getLogger(__name__)

class ProbabilisticGrounding(DocumentModel):
    def __init__(self,probabilictic_grounding_main_entry,**kwargs):

        super(ProbabilisticGrounding,self).__init__(**kwargs)
        implementations_path = os.path.realpath(probabilictic_grounding_main_entry.strip())
        parent, filename = os.path.split(implementations_path)
        _, package_name = os.path.split(parent)
        module_name = os.path.splitext(filename)[0]
        # inserting at 1 allows implementation to import abstract class
        sys.path.insert(1, parent)
        try:
            implementations = importlib.import_module(
                module_name, package=package_name)
        except ImportError as e:
            logger.critical('Implementations at {} could not be imported; '
                            'make sure that the file exists and is a python '
                            'module (sister to an __init__.py file).'
                            .format(os.path.join(parent, filename)))
            raise e

        pg_config = dict()

        pg_config['max_number_of_tokens_per_sentence'] = int(kwargs['max_number_of_tokens_per_sentence'])
        pg_config['which_ontology'] = kwargs['which_ontology']
        pg_config['keywords'] = kwargs['keywords']
        pg_config['stopwords'] = kwargs['stopwords']
        pg_config['event_ontology_yaml'] = kwargs['event_ontology_yaml']
        pg_config['internal_ontology_yaml'] = kwargs['internal_ontology_yaml']
        pg_config['grounding_mode'] = kwargs['grounding_mode']
        pg_config['embeddings'] = kwargs['embeddings']
        pg_config['exemplars'] = kwargs['exemplars']
        pg_config['bert_centroids'] = kwargs['bert_centroids']
        pg_config['bert_npz_file_list'] = kwargs['bert_npz_file_list']
        pg_config['n_best'] = int(kwargs['n_best'])
        pg_config['only_use_bert_from_root'] = implementations.str2bool(kwargs['only_use_bert_from_root'])
        pg_config['blacklist'] = kwargs['blacklist']
        pg_config['event_mention_typing_field'] = kwargs['event_mention_typing_field']
        pg_config['threshold'] = float(kwargs['threshold'])
        self.serifxml_grounder = implementations.SerifXMLGrounder(**pg_config)

    def process_document(self, serif_doc):
        self.serifxml_grounder.process_doc(serif_doc)