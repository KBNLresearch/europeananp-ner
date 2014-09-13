#!/usr/bin/env sh
clear;mvn clean;mvn package && (\
    cd ./test-files/;./test_commandLine_ner.sh;cd ..)
