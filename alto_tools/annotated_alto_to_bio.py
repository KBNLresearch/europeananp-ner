#!/usr/bin/env python

# Parse annotated ALTO to create a training-bio file.

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

def parse_alto_to_ner(filename):
    fh = open(filename, 'r')
    data = fh.read()
    fh.close()
    

    l = "" 
    d = []
    c = 0
    one = False
    for line in data.split('\n'):
	if line.strip().startswith('<string'):
	    entity = False
	    if line.find('ALTERNATIVE') > -1:
		entity = line.split("ALTERNATIVE='")[1].split("'")[0]
                if entity == "B-NOT KNOWN":
                    entity = False
                if entity == "B-MISC":
                    entity = False

                one = True
                c += 1
	    word = line.split('content="')[1].split('"')[0]
            a = { html.fromstring(word).text : entity } 
            d.append(a)

            if entity:
                l += " " + html.fromstring(word).text 
            else:
                l += " " + html.fromstring(word).text
            l = l.strip()

            if (l.endswith(".") or l.endswith("?") or l.endswith("!"))and one and len(l) > 40 and c > 1: 
                one = False
                c = 0
                for item in d:
                    if item[item.keys()[0]]:
                        print item.keys()[0] + " POS " + item.values()[0]
                    else:
                        print item.keys()[0] + " POS O"
                #print "\n"
                #print(l)
                l = ""
                d = []
            if (l.endswith(".") or l.endswith("?") or l.endswith("!")):
                l = ""
                d = []
                c = 0
                one = False

if len(sys.argv) <= 1:
    sys.stdout.write("No imput files given, usage alto_to_ner.py <files>\n")
    sys.exit(-1)

for filename in sys.argv[1:]:
    parse_alto_to_ner(filename)
