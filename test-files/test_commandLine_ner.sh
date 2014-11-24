#!/usr/bin/env bash


#
# Test script for Europeana Newspaper NER.
#
# This test invokes the compiled NER, and displays some stats on stdout. 
#
# LICENCE: See toplevel of project
# CREATOR: Willem Jan Faber
#


CLASSIFIER_DUTCH="eunews_dutch.crf.gz"
CLASSIFIER_GERMAN="eunews_german.crf.gz"

DEBUG=false

JAVA_BIN=`which java`
JAVA_EUNEWSNER="`ls ../target/*-SNAPSHOT-jar-with-dependencies.jar*`"

OUTPUTDIR="out"

if [ ! -f "$JAVA_EUNEWSNER" ]; then
    echo "Please run: mvn package in ../"
    exit
fi

clean_test() {
    if [ -f "$CLASSIFIER_DUTCH" ]; then
        rm $CLASSIFIER_DUTCH
    fi

    if [ -f "$CLASSIFIER_GERMAN" ]; then
        rm $CLASSIFIER_GERMAN
    fi

    if [ -d "$OUTPUTDIR" ]; then
        rm -rf out
    fi
}

test_creation() {
    case $1 in
    de)
        propfile="austen_german.prop"
        clasifierfile="$CLASSIFIER_GERMAN"
        ;;
    nl)
        propfile="austen_dutch.prop"
        clasifierfile="$CLASSIFIER_DUTCH"
        ;;
    esac

    cmd="$JAVA_BIN -Xmx5G -cp $JAVA_EUNEWSNER \
    edu.stanford.nlp.ie.crf.CRFClassifier -prop $propfile"

    echo "Generating new classification model. ($1)"
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
        echo `ls -lah ./"$clasifierfile"`
    else
        echo "error: $res :"
        echo "Some wierd things happend, run sh -x $0"
        echo "Or run: $cmd"
        exit 1
    fi
}

test_extraction() {
    case $1 in
    de)
        inpufile="german_alto.xml";
        modelname="$CLASSIFIER_GERMAN"
        ;;
    nl)
        inpufile="dutch_alto.xml";
        modelname="$CLASSIFIER_DUTCH"
        ;;
    esac

    cmd="$JAVA_BIN -jar $JAVA_EUNEWSNER -c alto -d out -f alto -l nl -m \
    nl=$modelname -n 8 $inpufile"

    echo "Applying generated model ($1)."
    # Result of the process should be "ok"
    if $DEBUG; then
        res=`($cmd) && echo "ok"`
        echo "res: '$res'"
        res=`echo $res | rev | cut -d ' ' -f 1 | rev`
        echo "res: '$res'"
    else
        res=`($cmd) 2>&1 > /dev/null && echo "ok"`
        res=`echo $res | rev | cut -d ' ' -f 1 | rev`
    fi

    if [ "$res" == "ok" ]; then
        LOCATION_COUNT=`cat "$OUTPUTDIR"/*/* | grep "ALTERNATIVE" | grep 'LOC" ' | wc -l`
        ORGANIZATION_COUNT=`cat "$OUTPUTDIR"/*/* | grep "ALTERNATIVE" | grep 'ORG" ' | wc -l`
        PERSON_COUNT=`cat "$OUTPUTDIR"/*/* | grep "ALTERNATIVE" | grep 'PER" ' | wc -l`
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


# Start with empty outputdir
clean_test

# Test the German classifier
time test_creation de
time test_extraction de

echo
clean_test

# Test the Dutch classifier
time test_creation nl
time test_extraction nl
