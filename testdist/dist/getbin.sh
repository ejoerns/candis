#!/bin/sh

COMMONS_URL="http://mirror.serversupportforum.de/apache//commons/cli/binaries/commons-cli-1.2-bin.zip"
wget -O commons.zip $COMMONS_URL
unzip commons.zip
rm commons.zip
