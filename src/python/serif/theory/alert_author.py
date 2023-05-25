from serif.theory.serif_theory import SerifTheory
from serif.xmlio import _SimpleAttribute


class ALERTAuthor(SerifTheory):
    author_id = _SimpleAttribute()
    author_canonical_name = _SimpleAttribute()
    age = _SimpleAttribute()
    profession = _SimpleAttribute()
    location = _SimpleAttribute()
    gender = _SimpleAttribute()
    personality = _SimpleAttribute()
    human = _SimpleAttribute()
    year_of_birth = _SimpleAttribute()
