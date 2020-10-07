import os
import sys
import json
import codecs

if __name__ == "__main__":
    filename="c:\\Users\\bmin\\Downloads\\wm_m12.v11.learnit_event.nn_event.json-ld"
    with codecs.open(filename, 'r', encoding='utf-8') as f:
        json_data = json.load(f)

