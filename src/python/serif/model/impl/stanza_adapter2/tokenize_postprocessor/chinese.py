# run this script as with /d4m/material/software/python/singularity/bin/singularity-python.sh -i python3.6-cuda10.0 run.py

import unittest

import regex

# before/after tokenization pairs
test_cases_str = '''
,3宗
, 3 宗
宗(
宗 (
).
) .
三,二十一
三 , 二十一
济.
济 .
:Edo10,
: Edo10 ,
Owo31
Owo31
说:
说 :
20%.
20% .
证,56400
证 , 56400
(US
( US
介绍,二〇〇六
介绍 , 二〇〇六
'''

# hand crafted rules for fixing Stanza 1.3.0 tokenizaion output on Chinese
rules = [

    # detach leading punctuations !.,:()[] from Chinese characters or numbers or English words
    (r'^([!.,:()\[\]])(\p{Han}|\d+|[a-zA-Z]+)', r'\1 \2'),

    # detach trailing punctuations !,:()[] from Chinese characters or numbers or %
    (r'(\p{Han}|\d+|%|[a-zA-Z]+)([!,:()\[\]])$', r'\1 \2'),

    # detach trailing period from anything before it
    (r'(\S+)(\.)$', r'\1 \2'),

    # detach numbers from Chinese text like 3宗 but not Edo10
    (r'^(\d+)(\p{Han})', r'\1 \2'),
    (r'(\p{Han})(\d+)$', r'\1 \2'),

    # split , in the middle of Chinese/numbers but not between numbers like 123,456
    (r'(\p{Han}+),(\p{Han}+|\d+)', r'\1 , \2',),
    (r'(\p{Han}+|\d+),(\p{Han}+)', r'\1 , \2',),
]


def apply_rules(rules, s):
    '''recursive apply fixing rules from the rules list to string s until no more changes can be made
    return list of tokens after rules are applied:

'''
    orig = s
    for pattern, replacement in rules:
        s = regex.sub(pattern, replacement, s)
    if orig != s:
        # recursively apply rules to sub tokens after changes
        ret = []
        for t in s.split(' '):
            ret.extend(apply_rules(rules, t))
        return ret
    else:
        return s.split(' ')


def fix_chinese(s):
    '''Apply fixing rules to string s, return fixed string.

    For example a ',3宗' => ', 3 宗'
'''
    x = apply_rules(rules, s)
    return ' '.join(x)


def test():
    test_cases = []
    a = [x for x in test_cases_str.split('\n') if x]
    for i in range(0, len(a), 2):
        test_cases.append((a[i], a[i + 1]))
        y = fix_chinese(a[i])
        assert y == a[i + 1], f'{a[i]} =>{y}, should be {a[i + 1]}'


if __name__ == "__main__":
    testcase = unittest.FunctionTestCase(test)
    runner = unittest.TextTestRunner()
    runner.run(testcase)
