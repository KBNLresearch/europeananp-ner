Named Entity Recognition Tool for <br>[Europeana Newspapers](http://www.europeana-newspapers.eu/) [![Build Status](https://secure.travis-ci.org/KBNLresearch/europeananp-ner.png?branch=master)](http://travis-ci.org/KBNLresearch/europeananp-ner)
------------------------------------------------------

This tool takes container documents ([MPEG21-DIDL](http://xml.coverpages.org/mpeg21-didl.html), [METS](http://www.loc.gov/standards/mets/)),
parses out all references to [ALTO](http://www.loc.gov/standards/alto/) files and tries to find named entities in the pages
(with most models: Location, Person, Organisation, Misc). The aim is to keep the physical location on the page available through the whole process
to be able to highlight the results in a viewer. Read more about it on the KBNLresearch [blog](http://researchkb.wordpress.com/2014/03/03/ner-newspapers/).

[Stanford NER](http://www-nlp.stanford.edu/software/CRF-NER.shtml) is used for tagging.

The following output formats are implemented:

* ALTO-with-Alternatives (a.k.a. inline ALTO)
* ALTO [2.1 (soon to be replaced with 3.0)](http://www.loc.gov/standards/alto/v3/alto-3-0.xsd)
* HTML
* CSV
* SQL


## Building

Building from source:

Install maven, java (version "1.7" and up). In the toplevel directory run:

    mvn package

This command will generate a jar, and a war version of the NER located in the target/ directory.
To deploy the war file, just copy the war file in your tomcat webapp directory. 

## Usage command-line-interface

Invoking help:

    java -jar NerAnnotator.jar --help

    usage: java -jar NerAnnotator.jar [OPTIONS] [INPUTFILES..]
    -c,--container <FORMAT>             Input type: mets (Default), didl,
                                        alto, text, html
    -d,--output-directory <DIRECTORY>   output DIRECTORY for result files.
                                        Default ./output
    -f,--export <FORMAT>                Output type: log (Default), csv,
                                        html, db, alto, alto2_1, bio.
                                        Multiple formats:" -f html -f csv"
    -l,--language <ISO-CODE>            use two-letter ISO-CODE for language
                                        selection: en, de, nl ....
    -m,--models <language=filename>     models for languages. Ex. -m
                                        de=/path/to/file/model_de.gz -m
                                        nl=/path/to/file/model_nl.gz
    -n,--nthreads <THREADS>             maximum number of threads to be used
                                        for processing. Default 8

    If there are no input files specified, a list of file names is read from stdin.

Example invocation for classification of german_alto.xml:

    java -Xmx800m -jar NerAnnotator.jar -c mets -f alto -l de -m de=./test-files/german.ser.gz -n 2 ./test-files/german_alto.xml

The given example takes the language model called 'german.ser.gz' and
applies it to 'german_alo.xml' using 2 threads, and container type METS.

## Usage web-interface

Webinterface standalone:

    mvn jetty:run

This will try to bind to port 8080, using jetty.

Once deployed to Tomcat the following applies. The default configuration (as well as test-classifiers)
reside in src/main/resources/config.ini, this file references the available classifiers.

See the provided sample for some default settings. The landing page of the application
will show the available options once invoked with the browser.  The config.ini and the
classifiers will end up in WEB-INF/classes/, after deployment.

### Working with classifiers and model generation

To be able to compare your results with a baseline we provide
some test files located in the 'test-files' directory.

To run a back to front test (in Linux) try this:

    cd test-files;./test_europeana_ner.sh

Output should look (something)like this:

    Generating new classification model. (de)
    -rw-rw-r-- 1 aloha aloha 1.4M Sep 11 15:55 ./eunews_german.crf.gz

    real	0m3.984s
    user	0m5.452s
    sys	0m0.235s
    Applying generated model (de).

    Results:
        Locations: 4
        Organizations: 0
        Persons: 1071

    real	0m13.512s
    user	0m17.771s
    sys	0m0.336s

    Generating new classification model. (nl)
    -rw-rw-r-- 1 aloha aloha 1.7M Sep 11 15:56 ./eunews_dutch.crf.gz

    real	0m8.816s
    user	0m10.437s
    sys	0m0.371s
    Applying generated model (nl).

    Results:
        Locations: 1
        Organizations: 8
        Persons: 0

    real	0m5.048s
    user	0m9.278s
    sys	0m0.233s

To generate a model, use the following command:

    cd test-files; java -Xmx5G -cp ../target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop austen_dutch.prop

This should result in a file called 'eunews_dutch.crf.gz'. The size of the generated classifier should be around 1MB.

To verify the NER software use the created classifier to process the provided example file.

    cd test-files; java -jar ../target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar -c alto -d out -f alto -l nl -m nl=./eunews_dutch.crf.gz -n 8 ./dutch_alto.xml

Resulting in a directory called 'out' containing XML ALTO files with inline annotation.

The austen.prop file (basic version) can be found here:

    http://nlp.stanford.edu/downloads/ner-example/austen.prop
