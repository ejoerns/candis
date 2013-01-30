#!/bin/sh

COMMONS_CLI_VERSION="1.2"

COMMONS_CLI_URL="http://mirror.serversupportforum.de/apache//commons/cli/binaries/commons-cli-$COMMONS_CLI_VERSION-bin.zip"

DIST_LIB="../dist/lib/"

mkdir -p ../dist/lib
if [ ! -e "$DIST_LIB/commons-cli-$COMMONS_CLI_VERSION/commons-cli-$COMMONS_CLI_VERSION.jar" ]
then
wget -O "$DIST_LIB/commons-cli.zip" "$COMMONS_CLI_URL"
unzip -oq -d $DIST_LIB "$DIST_LIB/commons-cli.zip"
rm "$DIST_LIB/commons-cli.zip"
fi

