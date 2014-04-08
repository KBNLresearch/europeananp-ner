#!/usr/bin/env python

# 
# ALTO2TEXT ALTO_TO_TEXT ALTO TO TEXT
# alto2text alto_to_text alto to text
# 

# Open an ALTO (xml) file and print the text to stdout.
# This is useful to compare the output of stanford-vanilla
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
import urllib

import xml.etree.ElementTree as ET

sys.stdout = codecs.getwriter(locale.getpreferredencoding())(sys.stdout)

def xml_to_xmltree(alto_filename):
    alto_file = open(alto_filename, "rb")
    sys.stdout.write("Converting %s to text\n" % alto_filename)
    alto_data = alto_file.read()

    try:
        xmltree_alto_data = ET.fromstring(alto_data)
    except ET.ParseError as e:
        sys.stdout.write("Failed parsing %s, aborting\n" % alto_filename)
        sys.stdout.write(e.message)
        sys.exit(-1)

    alto_file.close()
    return ET.fromstring(alto_data)

def get_textblock_range(xmltree_alto_data, start, end):
    blocks = []

    is_in_range = False

    for item in xmltree_alto_data.iter():
        if item.tag.endswith("TextBlock"):
            if is_in_range:
                blocks.append(item.attrib.get("ID"))
            if item.attrib.get("ID") == start:
                blocks.append(start)
                is_in_range = True
            if item.attrib.get("ID") == end:
                is_in_range = False
                blocks.append(end)

    if not end in blocks:
        return []

    return blocks

def alto_to_disk(alto_filename, blocks = [], blocks_range = False):
    xmltree_alto_data = xml_to_xmltree(alto_filename)

    if blocks_range:
        if len(get_textblock_range(xmltree_alto_data, blocks[0], blocks[1])) == 0:
            sys.stdout.write("Error: Could not find a range spanning from %s to %s, aborting\n" % (blocks[0], blocks[1]))
            usage()
    elif len(blocks) >0:
        for item in blocks:
            if len(get_textblock_range(xmltree_alto_data, item, item)) == 0:
                sys.stdout.write("Error: Could not find block %s, aborting\n" % item)
                usage()

        

    alto_text = u""
    prev_was_hyp = False

    total_words = 0
    block_words = 0

    if len(blocks) == 0:
        print_all_blocks = True
    else:
        print_all_blocks = False

    print_current_block = None

    for item in xmltree_alto_data.iter():
        if item.tag.endswith("TextBlock"):
            if item.attrib.get("ID") in blocks:
                print_current_block = item

        if item.tag.endswith("String"):
            if prev_was_hyp:
                if print_current_block != None or print_all_blocks:
                    alto_text += item.get("CONTENT")
                    block_words += 1
                prev_was_hyp = False
            else: 
                if print_current_block != None or print_all_blocks:
                    alto_text += " " + item.get("CONTENT")
                    block_words += 1
                total_words += 1

        if item.tag.endswith("HYP"):
            prev_was_hyp = True

        if item.tag.endswith("TextBlock"):
            if print_current_block != None and print_current_block != item:
                print_current_block = None
            if print_current_block != None or print_all_blocks:
                if len(alto_text) > 0:
                    alto_text += "\n"

    sys.stdout.write("Total number of words: %s\n" % str(total_words))
    sys.stdout.write("Block words: %s\n" % str(block_words))

    text_outputfilename = alto_filename.split(os.sep)[-1].split('.')[0] + ".txt"
    sys.stdout.write("Writing to %s\n" % (text_outputfilename))

    if os.path.isfile(text_outputfilename):
        sys.stdout.write("Warning: %s already exists, overwriting file\n" % text_outputfilename)
    text_outputfile = codecs.open(text_outputfilename, "wb", "utf-8")
    text_outputfile.write(alto_text)
    text_outputfile.close()
    sys.stdout.write("Wrote %s bytes to %s\n" % (str(len(alto_text)), text_outputfilename))

def alto_to_text():
    blocks = False
    if not sys.argv[1].startswith('--'):
        for item in sys.argv[1:]:
            if item.lower().startswith('http'):
                fetch_via_http = True
            else:
                fetch_via_http = False
                alto_files = glob.glob(sys.argv[1])
    else:
        blocks = sys.argv[1]
        if len(sys.argv) <= 2:
            usage()
        for item in sys.argv[2:]:
            if item.lower().startswith('http'):
                fetch_via_http = True
            else:
                fetch_via_http = False
                alto_files = glob.glob(sys.argv[2])

    if blocks:
        blocks = blocks.split('=')[1]
        if blocks.find('-') > -1 and blocks.find(',') <= -1:
            blocks = [blocks.split('-')[0], blocks.split('-')[1]]
            block_range = True
        else:
            blocks = blocks.split(',')
            block_range = False
    else:
        blocks = []
        block_range = False

    if len(alto_files) <= 0 and not fetch_via_http:
        sys.stdout.write("No ALTO files found in path %s\n" % sys.argv[1])
        usage()

    if fetch_via_http:
        for url in sys.argv[1:]:
            data = urllib.urlopen(url).read()
            filename = url.split('/')[-1].split('.')[0]
            print filename
    else:
        for alto_filename in alto_files:
            alto_to_disk(alto_filename, blocks, block_range)

def usage():
    sys.stdout.write("Usage: %s [--blocks=a,b,c --blocks=a-c] path_to_alto_files\n\n" % sys.argv[0])
    sys.stdout.write("The (optional) blocks parameter is used to extract only certain parts of the ALTO document.\n")
    sys.exit(-1)

if __name__ == "__main__":
    if len(sys.argv) > 1:
        alto_to_text()
    else:
        usage()
