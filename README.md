Named Entity Recognition Tool for <br>[Europeana Newspapers](http://www.europeana-newspapers.eu/) [![Build Status](https://secure.travis-ci.org/KBNLresearch/europeananp-ner.png?branch=master)](http://travis-ci.org/KBNLresearch/europeananp-ner)
------------------------------------------------------

This tool takes container documents ([MPEG21-DIDL](http://xml.coverpages.org/mpeg21-didl.html), [METS](http://www.loc.gov/standards/mets/)),
parses out all references to [ALTO](http://www.loc.gov/standards/alto/) files and tries to find named entities in the pages
(with most models: Location, Person, Organisation, Misc). The aim is to keep the physical location on the page available through the whole process
to be able to highlight the results in a viewer.

Currently, [Stanford NER](http://www-nlp.stanford.edu/software/CRF-NER.shtml) is used for tagging.

At the moment, the following output formats are implemented:

* ALTO-with-Alternatives (this will be changed to valid [ALTO 2.1](https://github.com/altoxml/documentation/wiki/v2.1-Draft) once officially signed-off)
* HTML
* CSV (comma separated values)
* SQL

Basic usage:

Help:
  
     java -jar NerAnnotator.jar --help
	
Print result to stdout for German language:

     java -Xmx800m -jar NerAnnotator.jar -c mets -f alto -l de -m de=/path/to/trainingmodels/german/hgc_175m_600.crf.ser.gz -n 2 /path/to/mets/AZ_19260425/AZ_19260425_mets.xml
