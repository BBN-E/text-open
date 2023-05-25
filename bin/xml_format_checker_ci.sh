#!/usr/bin/env bash

set -e
set -o pipefail
set -u

MVN="/opt/apache-maven-3.3.9/bin/mvn"
JAVA_HOME="/opt/jdk1.8.0_201-x86_64"
REPO_ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && cd .. && pwd )"
PYTHON3="/opt/Python-3.5.2-x86_64/bin/python3"
MVN_SETTING="$CI_PROJECT_DIR/m2_settings.xml"

tmp=`mktemp -d`

echo $tmp

trap "trap - SIGTERM && kill 0" SIGINT SIGTERM

cd $REPO_ROOT/src/python && env PYTHONPATH=$REPO_ROOT/src/python LOGLEVEL=DEBUG $PYTHON3 serif/driver/pipeline.py $REPO_ROOT/src/python/config/regtest test/rundir/output/input_list.txt $tmp

cd $REPO_ROOT/src/java/serif && env JAVA_HOME=$JAVA_HOME $MVN --settings $MVN_SETTING clean install -am && env JAVA_HOME=$JAVA_HOME $MVN --settings $MVN_SETTING exec:java -Dexec.mainClass="com.bbn.serif.util.SerifXMLPeek" -Dexec.args="originalText $tmp/20190712162216-0.xml"

exit $?