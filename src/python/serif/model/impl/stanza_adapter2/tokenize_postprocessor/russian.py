# run this script as with /d4m/material/software/python/singularity/bin/singularity-python.sh -i python3.6-cuda10.0 run.py

import unittest

import regex

# before/after tokenization pairs
test_cases_str = '''
2014.
2014 .
ад.
ад .
-Новый
- Новый
'''

# hand crafted rules for fixing Stanza 1.3.0 tokenizaion output on Russian
rules = [
    # detach trailing period from anything before it
    (r'(\S+)(\.)$', r'\1 \2'),

    # detach leading - from words
    (r'^(-)(\w+)', r'\1 \2'),

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


def fix_russian(s):
    '''Apply fixing rules to string s, return fixed string.
'''
    x = apply_rules(rules, s)
    return ' '.join(x)


def test():
    test_cases = []
    a = [x for x in test_cases_str.split('\n') if x]
    for i in range(0, len(a), 2):
        test_cases.append((a[i], a[i + 1]))
        y = fix_russian(a[i])
        assert y == a[i + 1], f'{a[i]} =>{y}, should be {a[i + 1]}'


if __name__ == "__main__":
    testcase = unittest.FunctionTestCase(test)
    runner = unittest.TextTestRunner()
    runner.run(testcase)
