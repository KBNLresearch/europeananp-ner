Named Entity Recognition Tool for [Europeana Newspapers](http://www.europeana-newspapers.eu/)
------------------------------------------------------

This tool takes container documents ([MPEG21-DIDL](http://xml.coverpages.org/mpeg21-didl.html), [METS](http://www.loc.gov/standards/mets/)), parses out all references to [ALTO](http://www.loc.gov/standards/alto/)
files and tries to find named entities in the pages (with most models: Location, Person, 
Organisation, Misc).
The aim is to keep the physical location on the page available through the whole process 
to be able to highlight the results in a viewer.

[Stanford NER](http://www-nlp.stanford.edu/software/CRF-NER.shtml) is used for tagging.

See help for supported output formats. Basic usage:

Help:
  
     java -jar NerAnnotater --help
	
Print result to stdout for German language:
  
     java -Xmx800m -jar NerAnnotater.jar -l de -m de=/path/to/trainingmodels/german/hgc_175m_600.crf.ser.gz -n 8 /path/to/mets/AZ_19260425/AZ_19260425_mets.xml