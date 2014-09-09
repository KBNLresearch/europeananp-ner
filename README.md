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

Building from source:

Install maven, java (version "1.7" and up). In the toplevel directory run:

    mvn package

This command will generate a jar, and a war version of the NER located in the target/ directory.
To deploy the war file, just copy the war file in your tomcat webapp directory. To test the webapp
from command line (this will try to bind to port 8080): 

    mvn jetty:run

Basic usage (Command line client): 

Help:
  
      java -jar NerAnnotator.jar --help
	
Print result to stdout for German language:

     java -Xmx800m -jar NerAnnotator.jar -c mets -f alto -l de -m de=/path/to/trainingmodels/german/hgc_175m_600.crf.ser.gz -n 2 /path/to/mets/AZ_19260425/AZ_19260425_mets.xml

### Training classifiers

To be able to compare your results with a baseline we provide you with some test files located in the 'test-files' directory.
    
Run the following command:

    java -Xmx5G -cp target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop test-files/austen_dutch.prop

This should result in a file called 'eunews_dutch.crf.gz' located in the directory 'test-files'. The size of the generated classifier should be around 1MB.

To verify the NER software use the created classifier to process the provided example file.

    java -jar target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar -c alto -d out -f alto -l nl -m nl=./test-files/eunews_dutch.crf.gz -n 8 ./test-files/dutch_alto.xml

Now you can compare the output with the example output provided.

    diff out/dutch_alto.xml-annotations/dutch_alto.xml.alto.xml ./test-files/dutch_alto_processed_output.xml

The same procedure can be applied using the German example files.

The austen.prop file (basic version) can be found here:

    http://nlp.stanford.edu/downloads/ner-example/austen.prop

Basic usage (Web client):

Build the NER package from source, (mvn package), and place the generated WAR file in the webapps dir:

    cp ./target/NerAnnotator-0.0.2-SNAPSHOT.war /usr/local/tomcat7/webapps/

The default configuration (as well as test-classifiers) resides in here: src/main/resources/config.ini, this file references the available classifiers. See the provided sample for some default settings.
The landing page of the application will show the available options once invoked with the browser. Once the webapp is deployed, the config.ini and the classifiers will end up in WEB-INF/classes/ (for now at least).
