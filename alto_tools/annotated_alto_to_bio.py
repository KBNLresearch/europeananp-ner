#!/usr/bin/env python

# Parse annotated ALTO(xml) to create a training-BIO(text) file,
# that can be used to train the eunews-NER tool. See the 'training'
# section in the provided README.md.

# Takes in annotated ALTO and parse it to BIO form:
#
# In POS O 
# Nederland POS B-LOC
# staat POS O
# een POS O
# huis POS O
# van POS O
# Beatrix POS B-PER

# The annotated ALTO can be created using the eunews-NER tool,
# with the '-f alto' option.

# Example usage :
# 
# 1: Create classifier for dutch
# java -Xmx5G -cp target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar edu.stanford.nlp.ie.crf.CRFClassifier -prop test-files/austen_dutch.prop
#
# 2: Classify dutch ALTO and output it in annotated ALTO format.
# java -jar target/NerAnnotator-0.0.2-SNAPSHOT-jar-with-dependencies.jar -c alto -d out -f alto -l nl -m nl=./test-files/eunews_dutch.crf.gz -n 8 ./test-files/dutch_alto.xml

# 3: Parse the annotated ALTO output into bio format and store the results.
# python alto_tools/annotated_alto_to_bio.py ./out/dutch_alto.xml-annotations/dutch_alto.xml.alto.xml > dutch.bio

#  Copyright (c) 2013 Koninklijke Bibliotheek
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the European Union Public Licence (EUPL),
#  version 1.1 (or any later version).
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  European Union Public Licence for more details.
#
#  You should have received a copy of the European Union Public Licence
#  along with this program. If not, see
#  http://www.osor.eu/eupl/european-union-public-licence-eupl-v.1.1

import sys
import codecs
import locale

import xml.etree.ElementTree as ET
from lxml import html


sys.stdout = codecs.getwriter(locale.getpreferredencoding())(sys.stdout) 


MIN_CHAR_PER_LINE = 20
MIN_ENTITY_REQ = 1


def annotated_alto_to_bio(filename):
    fh = codecs.open(filename,"r","utf-8") #open(filename, 'r')
    alto_data = fh.read()
    fh.close()
    
    entity_count = 0
    foundone = False
    sentence = "" 
    sentence_mapping = []

    for line in alto_data.split('\n'):
	if line.strip().lower().startswith('<string'):
	    entity = False

	    if line.find('ALTERNATIVE') > -1:


                if line.find("ALTERNATIVE=\"") > -1:
                    entity = line.split("ALTERNATIVE=\"")[1].split("\"")[0]

                if line.find("ALTERNATIVE='") > -1:
                    entity = line.split("ALTERNATIVE='")[1].split("'")[0]


                if entity == "B-NOT KNOWN":
                    entity = False
                if entity == "B-MISC":
                    entity = False
                foundone = True
                entity_count += 1


            if line.find('content="') > -1:
                word = line.split('content="')[1].split('"')[0]
            if line.find('CONTENT="') > -1:
                word = line.split('CONTENT="')[1].split('"')[0]
            word_entity_mapping = { html.fromstring(word).text : entity } 
            sentence_mapping.append(word_entity_mapping)

            if entity:
                sentence += u" " + html.fromstring(word).text.strip()
            else:
                sentence += u" " + html.fromstring(word).text.strip()

            if (sentence.endswith(".") or sentence.endswith("?") or sentence.endswith("!")) \
                    and foundone and len(sentence) > MIN_CHAR_PER_LINE and entity_count > MIN_ENTITY_REQ: 
                # If the sentence is finished, and contains at least MIN_CHAR_PER_LINE characters,
                # and the nr of entities is greater then MIN_ENTITY_REQ, add the line to the output.

                for item in sentence_mapping:
                    if item[item.keys()[0]]:
                        print item.keys()[0] + " POS " + item.values()[0]
                    else:
                        print item.keys()[0] + " POS O"
                entity_count = 0
                foundone = False
                sentence = ""
                sentence_mapping = []

            if (sentence.endswith(".") or sentence.endswith("?") or sentence.endswith("!")):
                # If the sentence is finished, reset the variables to continue with the next sentence.
                entity_count = 0
                foundone = False
                sentence = ""
                sentence_mapping = []

if len(sys.argv) <= 1:
    sys.stdout.write("No imput files given, usage alto_to_ner.py <files>\n")
    sys.exit(-1)

for filename in sys.argv[1:]:
    annotated_alto_to_bio(filename)
