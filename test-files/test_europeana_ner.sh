#!/usr/bin/env bash

#
# Test script for Europeana Newspaper NER
#

DEBUG=false

JAVA_BIN=`which java`
JAVA_EUNEWSNER="../target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar"

if [ ! -f "$JAVA_EUNEWSNER" ]; then
    echo "Please run: mvn package in ../"
fi

clean_test()
{
    if [ -f "eunews_dutch.crf.gz" ]; then
        rm eunews_dutch.crf.gz
    fi

    if [ -d "out" ]; then
        rm -rf out
    fi
}

test_creation()
{
    cmd="$JAVA_BIN -Xmx5G -cp $JAVA_EUNEWSNER edu.stanford.nlp.ie.crf.CRFClassifier -prop austen_dutch.prop"

    echo "Generating new classification model."
    # Result of the process should be "ok"
    if $DEBUG; then
        res=`($cmd) 2>&1 && echo "ok"`
        echo "res: '$res'"
        res=`echo $res | rev | cut -d ' ' -f 1 | rev`
        echo "res: '$res'"
    else
        res=`($cmd) 2>&1 > /dev/null && echo "ok"`
        res=`echo $res | rev | cut -d ' ' -f 1 | rev`
    fi

    if [ "$res" == "ok" ]; then
        model_size=`ls -lah ./eunews_dutch.crf.gz`
    else
        echo "error: $res :"
        echo "Some wierd things happend, run sh -x $0"
        echo "Or run: $cmd"
        exit 1
    fi
}

test_extraction()
{
    cmd="$JAVA_BIN -jar $JAVA_EUNEWSNER -c alto -d out -f alto -l nl -m \
    nl=eunews_dutch.crf.gz -n 8 dutch_alto.xml"

    echo "Applying generated model."
    # Result of the process should be "ok"
    if $DEBUG; then
        res=`($cmd)`
        echo "res: '$res'"
        res=`echo $res | rev | cut -d ' ' -f 1 | rev`
        echo "res: '$res'"
    else
        (
        res=`($cmd) 2>&1 > /dev/null && echo "ok"`
        res=`echo $res | rev | cut -d ' ' -f 1 | rev`
        ) 2>&1 > /dev/null
    fi

    if [ "$res" == "ok" ]; then
        LOCATION_COUNT=`cat o*/d*/* | grep "ALTERNATIVE" | grep 'LOC" ' | wc -l`
        ORGANIZATION_COUNT=`cat o*/d*/* | grep "ALTERNATIVE" | grep 'ORG" ' | wc -l`
        PERSON_COUNT=`cat o*/d*/* | grep "ALTERNATIVE" | grep 'PER" ' | wc -l`
    else
        echo "Some wierd things happend, run sh -x $0"
        echo "Or run: $cmd"
        exit 1
    fi

    echo -e "\nResults:"
    echo -e "\tLocations: $LOCATION_COUNT"
    echo -e "\tOrganizations: $ORGANIZATION_COUNT"
    echo -e "\tPersons: $PERSON_COUNT"
}

clean_test
test_creation
test_extraction
