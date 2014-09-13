Named Entity Recognition Tool for <br>[Europeana Newspapers](http://www.europeana-newspapers.eu/) [![Build Status](https://secure.travis-ci.org/KBNLresearch/europeananp-ner.png?branch=master)](http://travis-ci.org/KBNLresearch/europeananp-ner)
------------------------------------------------------

This tool takes container documents ([MPEG21-DIDL](http://xml.coverpages.org/mpeg21-didl.html), [METS](http://www.loc.gov/standards/mets/)),
parses all references to [ALTO](http://www.loc.gov/standards/alto/) files and tries to find named entities in the pages
(with most models: Location, Person, Organisation, Misc). The aim is to keep the physical location on the page available through the whole process
to be able to highlight the results in a viewer.

Read more about it on the KBNLresearch [blog](http://researchkb.wordpress.com/2014/03/03/ner-newspapers/).

[Stanford NER](http://www-nlp.stanford.edu/software/CRF-NER.shtml) is used for tagging. The goal during development was to use 'loose coupling',
this enables us to quickly inherit/benefit from upstream development. Most of the development is done at the research department of the KB,
[national library of the Netherlands](http://kb.nl/en/research). If you are looking for a project which does more interaction with the core of
Stanford-NER, take a peek at the project from our colleagues INL,
[Institute for Dutch Lexicology](http://www.inl.nl/our-work-and-working-methods) [INL-NERT](https://github.com/INL/NERT),
although they are separate branches now, there is a desire to integrate both in the future.

## Input formats

The following input formats are implemented:

* ALTO 1.0
* HTML
* Mets
* MPEG21 DIDL
* Text

## Output formats

The following output formats are implemented:

* ALTO [2.1 (soon to be replaced with 3.0)](http://www.loc.gov/standards/alto/v3/alto-3-0.xsd)
* ALTO-with-Alternatives (aka. inline ALTO)
* BIO
* CSV
* HTML
* SQL(db)

## Building

Building from source:

Install Maven, Java (v1.7 and up). Clone the source from github, and in the toplevel directory run:

    mvn package

This command will generate a JAR and a WAR of the NER located in the `target/` directory.
To deploy the WAR, just copy it into the Tomcat webapp directory, or use Tomcat
manager to do it for you.

Or move quickly and run (on \*nix systems):

    git clone https://github.com/KBNLresearch/europeananp-ner.git
    cd europeananp-ner/
    ./go.sh

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

This will try to bind to port 8080, using Jetty.

Once deployed to Tomcat the following applies. The default configuration (as well as test-classifiers)
reside in `src/main/resources/config.ini`, this file references the available classifiers.

See the provided sample for some default settings. The landing page of the application
will show the available options once invoked with the browser.  The config.ini and the
classifiers will end up in `WEB-INF/classes/`, after deployment.

### Working with classifiers and binary model generation

To be able to compare your results with a baseline we provide
some test files located in the ```test-files``` directory.

To run a back-to-front test try:

    cd test-files;./test_europeana_ner.sh

The output should look something like:

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

To generate a binary classification model, use the following command:

    cd test-files; java -Xmx5G -cp ../target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop austen_dutch.prop

This should result in a file called `eunews_dutch.crf.gz`, with a file-size of +/- 1MB.

To verify the NER software, use the created classifier to process the provided example file.

    cd test-files; java -jar ../target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar -c alto -d out -f alto -l nl -m nl=./eunews_dutch.crf.gz -n 8 ./dutch_alto.xml

Resulting in a directory called `out` containing ALTO files with inline annotation.

### General remarks on binary classification model generation

The process of generating a binary classification model is a delicate one. The input
`.bio` file needs be as clean as possible to prevent the garbage in-out rule from
happening. Thus, use noise filters while creating `.bio` files.

Gazette's greatly improve the quality of your classification process, but a
big model in memory may slow down processing speed. Overall there is a strong
correlation in model size and performance.

The Stanford NER package offers a lot of settings that can influence the
binary model generation process. These settings can be configured using
`austen.prop`, For more information on the Stanford settings see
[Stanford NER FAQ](http://nlp.stanford.edu/software/crf-faq.shtml).

Binary classification models generated with this tool are fully compatible with the upstream
version of the Stanford NER.
