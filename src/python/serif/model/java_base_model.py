import io, typing
from serif.model.base_model import BaseModel
from serif.theory.document import Document


class JavaBaseModel(BaseModel):
    def __init__(self, **kwargs):
        super(JavaBaseModel, self).__init__(**kwargs)


