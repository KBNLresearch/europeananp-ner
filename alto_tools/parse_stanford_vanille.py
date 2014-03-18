#!/usr/bin/env python

# Read the output from the stanford tagger,
# and print the named entities only.

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

if len(sys.argv) > 1:
    stanford_file = open(sys.argv[1], "r")
    stanford_data = stanford_file.read()
    for line in stanford_data.split(' '):
        if not line.endswith("/O") and len(line.strip()) > 0:
            print line
else:
    sys.stdout.write("Missing inputfile name")
