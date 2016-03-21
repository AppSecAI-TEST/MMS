#!/bin/bash

# -======================-
# User configurable values
# -======================-

export INSTALLDIR="$(dirname $0)"

#------------------------------------------------------------------
# You can adjust the Java minimum and maximum heap space here.
# Just change the Xms and Xmx options. Space is given in megabyte.
#    '-Xms64M' sets the minimum heap space to 64 megabytes
#    '-Xmx512M' sets the maximum heap space to 512 megabytes
#------------------------------------------------------------------
export JAVA_OPTS="-Xmx2048M"
export JAVA_EXE="$(which java)"

# -======================-
# Other values
# -======================-

export LIBDIR="$INSTALLDIR"/lib
export OLD_CLASSPATH="$CLASSPATH"
CLASSPATH="$INSTALLDIR/*:$INSTALLDIR"

"$JAVA_EXE" "$JAVA_OPTS" -classpath "$CLASSPATH" com.bc.fiduceo.ingest.IngestionToolMain "$@"

export CLASSPATH="$OLD_CLASSPATH"