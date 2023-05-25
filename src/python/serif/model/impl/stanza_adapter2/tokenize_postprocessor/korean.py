# run this script as with /d4m/material/software/python/singularity/bin/singularity-python.sh -i python3.6-cuda10.0 run.py

import unittest

import regex

# before/after tokenization pairs
test_cases_str = '''
UNCMAC)
UNCMAC )
답(
답 (
질문12
질문 12
브(
브 (
Edo10
Edo10
'''

# hand crafted rules for fixing Stanza 1.3.0 tokenizaion output on Korean
rules = [

    # detach leading punctuations !.,:()[] from Korean text, numbers or words
    (r'^([!.,:()\[\]])(\p{Hangul}|\d+|[a-zA-Z]+)', r'\1 \2'),

    # detach trailing punctuations !,:()[] from Korean characters or numbers or %
    (r'(\p{Hangul}|\d+|%|[a-zA-Z]+)([!,:()\[\]])$', r'\1 \2'),

    # detach trailing period from anything before it
    (r'(\S+)(\.)$', r'\1 \2'),

    # detach numbers from Korean text like 질문12 but not Edo10
    (r'^(\d+)(\p{Hangul})', r'\1 \2'),
    (r'(\p{Hangul})(\d+)$', r'\1 \2'),

    # split , in the middle of Korean/numbers but not between numbers like 123,456
    (r'(\p{Hangul}+),(\p{Hangul}+|\d+)', r'\1 , \2',),
    (r'(\p{Hangul}+|\d+),(\p{Hangul}+)', r'\1 , \2',),
]


def apply_rules(rules, s):
    '''apply fixing rules from the rules list to string s
    return list of tokens after rules are applied

    Unlike Chinese, it seems simply apply the rules once will fix most things
    so we do not run the rules recursively.
'''
    for pattern, replacement in rules:
        s = regex.sub(pattern, replacement, s)
    return s.split(' ')


def fix_korean(s):
    '''Apply fixing rules to string s, return fixed string.
'''
    x = apply_rules(rules, s)
    return ' '.join(x)


def test():
    test_cases = []
    a = [x for x in test_cases_str.split('\n') if x]
    for i in range(0, len(a), 2):
        test_cases.append((a[i], a[i + 1]))
        y = fix_korean(a[i])
        assert y == a[i + 1], f'{a[i]} =>{y}, should be {a[i + 1]}'


if __name__ == "__main__":
    testcase = unittest.FunctionTestCase(test)
    runner = unittest.TextTestRunner()
    runner.run(testcase)
