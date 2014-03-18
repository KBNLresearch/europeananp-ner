#!/usr/bin/env python

# Open an ALTO file an print the text to stdout.
# This is usefull to compare the output of stanford-vanille 
# against the europeana-ner.

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

import os
import sys
import glob
import codecs
import locale

import xml.etree.ElementTree as ET

sys.stdout = codecs.getwriter(locale.getpreferredencoding())(sys.stdout)

def alto_to_text(argv):
    alto_files = glob.glob(argv[1])

    if len(alto_files) <= 0:
        sys.stdout.write("No ALTO files found in path %s\n" % sys.argv[1])
        sys.stdout.write("Usage: %s path_to_alto_files\n" % sys.argv[0])
        

    for alto_filename in alto_files:
        alto_file = open(alto_filename, "rb")
        sys.stdout.write("Converting %s to text\n" % alto_filename)
        alto_data = alto_file.read()

        try:
            xmltree_alto_data = ET.fromstring(alto_data)
        except ET.ParseError as e:
            sys.stdout.write("Failed parsing %s, aborting\n" % alto_filename)
            print e.message
            sys.exit(-1)

        xmltree_alto_data = ET.fromstring(alto_data)
        alto_file.close()
        root = xmltree_alto_data
        alto_text = u""
        prev_was_hyp = False
        total_words = 0

        for item in root.iter():
            if item.tag.endswith("String"):
                if prev_was_hyp:
                    alto_text += item.get("CONTENT")
                    prev_was_hyp = False
                else: 
                    alto_text += " " + item.get("CONTENT")
                    total_words += 1
            if item.tag.endswith("HYP"):
                prev_was_hyp = True
            if item.tag.endswith("TextBlock"):
                alto_text += "\n"

        sys.stdout.write("Number of words: %s\n" % str(total_words))

        text_outputfilename = alto_filename.split('.')[1].split(os.sep)[-1] + ".txt"
        if not os.path.isfile(text_outputfilename):
            text_outputfile = codecs.open(text_outputfilename, "wb", "utf-8")
            text_outputfile.write(alto_text)
            text_outputfile.close()
            sys.stdout.write("Wrote %s bytes to %s\n" % (str(len(alto_text)), text_outputfilename))
        else:
            sys.stdout.write("Error: %s allready exists, did not overwrite file\n" % text_outputfilename)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        alto_to_text(sys.argv)
    else:
        sys.stdout.write("Usage: %s path_to_alto_files\n" % sys.argv[0])

