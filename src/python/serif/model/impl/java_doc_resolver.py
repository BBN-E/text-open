import os,sys,io
from serif.model.java_base_model import JavaBaseModel
from serifxml3 import Document
from jnius import autoclass



class JavaDocResolverModel(JavaBaseModel):
    def __init__(self,**kwargs):
        super(JavaDocResolverModel,self).__init__(**kwargs)
        self.java_class_ins = dict()
        self.java_class_dir = dict()
        self.java_class_dir['DocTheoryResolver'] = autoclass('com.bbn.serif.util.resolver.DocTheoryResolver')
        Parameters = autoclass('com.bbn.bue.common.parameters.Parameters')

        if "doc_resolver_par_path" in kwargs:
            java_params = Parameters.loadSerifStyle(autoclass('java.io.File')(kwargs["doc_resolver_par_path"]))
            self.java_class_ins['docTheoryResolver'] = self.java_class_dir['DocTheoryResolver'](java_params)
        else:
            parameter_builder = Parameters.builder()
            for k,v in kwargs.items():
                if isinstance(k,str) and isinstance(v,str):
                    parameter_builder.set(k,v)
                    if "resolvers" in k:
                        self.resolvers = v
            self.java_class_ins['docTheoryResolver'] = self.java_class_dir['DocTheoryResolver'](parameter_builder.build())



        getBytesFromDocTheory = self.java_class_dir['DocTheoryResolver'].getBytesFromDocTheory
        getDocTheoryFromBytes = self.java_class_dir['DocTheoryResolver'].getDocTheoryFromBytes

        self.get_java_doc_theory_from_py_bytes = getDocTheoryFromBytes
        self.get_py_bytes_from_java_doc_theory = getBytesFromDocTheory

    def convert_doc_theory_python_to_java(self, serif_doc):
        assert isinstance(serif_doc, Document)
        mem_file = io.BytesIO()
        serif_doc.save(mem_file)
        mem_file.seek(0)
        serif_doc_java_in = self.get_java_doc_theory_from_py_bytes(mem_file.read())
        return serif_doc_java_in

    def convert_doc_theory_java_to_python(self, serif_doc_java):
        xml_string = bytes(self.get_py_bytes_from_java_doc_theory(serif_doc_java)).decode("utf-8")
        serif_doc = Document(xml_string)
        return serif_doc

    def process(self, serif_doc):
        serif_doc_java_in = self.convert_doc_theory_python_to_java(serif_doc)
        serif_doc_java_out = self.java_class_ins['docTheoryResolver'].resolve(serif_doc_java_in)
        serif_doc_out = self.convert_doc_theory_java_to_python(serif_doc_java_out)
        return serif_doc_out