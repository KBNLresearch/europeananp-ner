Named Entity Recognition Tool for <br>[Europeana Newspapers](http://www.europeana-newspapers.eu/) [![Build Status](https://secure.travis-ci.org/KBNLresearch/europeananp-ner.png?branch=master)](http://travis-ci.org/KBNLresearch/europeananp-ner)
------------------------------------------------------

This tool takes container documents ([MPEG21-DIDL](http://xml.coverpages.org/mpeg21-didl.html), [METS](http://www.loc.gov/standards/mets/)),
parses out all references to [ALTO](http://www.loc.gov/standards/alto/) files and tries to find named entities in the pages
(with most models: Location, Person, Organisation, Misc). The aim is to keep the physical location on the page available through the whole process
to be able to highlight the results in a viewer. Read more about it on the KBNLresearch [blog](http://researchkb.wordpress.com/2014/03/03/ner-newspapers/).

Currently, [Stanford NER](http://www-nlp.stanford.edu/software/CRF-NER.shtml) is used for tagging.

At the moment, the following output formats are implemented:

* ALTO-with-Alternatives
* ALTO [2.1](https://github.com/altoxml/documentation/wiki/v2.1-Draft)
* HTML
* CSV (comma separated values)
* SQL

Basic usage:

Help:
  
     java -jar NerAnnotator.jar --help
	
Print result to stdout for German language:

     java -Xmx800m -jar NerAnnotator.jar -c mets -f alto -l de -m de=/path/to/trainingmodels/german/hgc_175m_600.crf.ser.gz -n 2 /path/to/mets/AZ_19260425/AZ_19260425_mets.xml

Training classifiers:

    To be able to compare your results with a base-line we've provided some test files located in the 'test-files' directory.
    Running the following command :

        java -Xmx5G -cp target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop test-files/austen_dutch.prop

    Should result in a file called 'eunews_dutch.crf.gz' located in the directory 'test-files', the size of the classifier should be around 1Mb.

    To verify the NER software use the created classifier to process the provided example file.

        java -jar target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar -c alto -d out -f alto -l nl -m nl=./test-files/eunews_dutch.crf.gz -n 8 ./test-files/dutch_alto.xml

    Compare the output with te provided example output.

        diff out/dutch_alto.xml-annotations/dutch_alto.xml.alto.xml ./test-files/dutch_alto_processed_output.xml
        5972d5971
        < <!-- urn=ddd:000014177:mpeg21:p001:alto -->

    This same prcedure can be done with the German example files.

The austen.prop file (basic version) can be found here:

    http://nlp.stanford.edu/downloads/ner-example/austen.prop
