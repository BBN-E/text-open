import bz2
import gzip
import io
import pathlib
import sys


def fopen(filename, mode='rt', encoding='utf-8', **kwargs):
    '''Drop-in replacement for built in open() so that .gz and .bz2 files can be
    handled transparently. If filename is '-', standard input will be used.

    Since we are mostly dealing with text files UTF-8 encoding is used by default.
    '''

    if isinstance(filename, pathlib.PurePath):
        filename = str(filename)

    if isinstance(filename, io.IOBase):
        return filename

    if filename == '-':
        if 'w' in mode:
            return io.TextIOWrapper(sys.stdout.buffer, encoding=encoding)
        else:
            return io.TextIOWrapper(sys.stdin.buffer, encoding=encoding)

    if filename.endswith(".gz"):
        _fopen = gzip.open
        if 'b' not in mode and 't' not in mode:
            mode = mode + 't'  # 'rb' is the default for gzip and bz2
    elif filename.endswith(".bz2"):
        _fopen = bz2.open
        if 'b' not in mode and 't' not in mode:
            mode = mode + 't'
    else:
        _fopen = open
    if 'b' in mode:
        return _fopen(filename, mode=mode, **kwargs)
    else:
        return _fopen(filename, mode=mode, encoding=encoding, **kwargs)
