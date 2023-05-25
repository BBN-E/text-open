#!/usr/bin/env bash

set -e
set -o pipefail
set -u

REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && cd .. && pwd )"
PYTHON3="/opt/Python-3.5.2-x86_64/bin/python3"

tmp=`mktemp -d`

echo $tmp

trap "trap - SIGTERM && kill 0" SIGINT SIGTERM

cd $REPO_ROOT/src/python && LOGLEVEL=DEBUG $PYTHON3 misc/serifxml_round_trip.py $REPO_ROOT/src/python/test/sample_doc.xml $tmp/sample_doc.out.xml

exit $?
