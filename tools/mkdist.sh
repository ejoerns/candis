#!/bin/sh
set -e

DIST="../dist/"

function addjar {
cd "../$1"
ant $2
cp "$3" "$DIST/$4"
cd "$OLDPWD"
}


addjar distributed "clean release" bin/classes.jar lib/candis.distributed.jar
addjar master "clean jar" dist/master.jar candis.master.jar
addjar testdist "clean jar" dist/testdist.jar candis.testdist.jar
addjar common "clean release" bin/classes.jar lib/candis.common.jar
