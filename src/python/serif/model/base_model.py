from abc import ABC
from abc import abstractmethod

class BaseModel(ABC):
    def __init__(self,**kwargs):
        pass

    @abstractmethod
    def process(self, serif_doc):
        pass

    def reload_model(self):
        pass