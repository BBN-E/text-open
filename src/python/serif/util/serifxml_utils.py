import os

class CountryIdentifier:
    initialized = False
    lowercase_country_strings = set()
    
    @classmethod 
    def load_countries_list(cls):
        script_dir = os.path.dirname(os.path.realpath(__file__))
        countries_list = os.path.realpath(os.path.join(
            script_dir, os.pardir, os.pardir, os.pardir, "java", "serif", "src", "main",
            "resources", "com", "bbn", "serif", "coreference", 
            "representativementions", "nationality-canonical-names"))
        with open(countries_list) as c:
            for line in c:
                line = line.strip()
                if len(line) == 0 or line.startswith("#"):
                    continue
                pieces = line.split(":", 1)
                CountryIdentifier.lowercase_country_strings.add(pieces[1].lower())
                CountryIdentifier.initialized = True

    @classmethod 
    def is_country_string(cls, s):
        if not CountryIdentifier.initialized:
            CountryIdentifier.load_countries_list()
        return s.lower() in CountryIdentifier.lowercase_country_strings
    
