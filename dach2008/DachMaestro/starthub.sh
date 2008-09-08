#!/bin/sh

source $HOME/.bashrc

# Check setting of DACHMAESTRO_HOME
if [ -z "$DACHMAESTRO_HOME" ];  then
    echo "please set DACHMAESTRO_HOME to the location of your Maestro installation" 1>&2
    exit 1
fi

# Jar-files from library.
LIBCLASSPATH=""
add_to_libclasspath () {
    JARFILES=`cd "$1" && ls *.jar 2>/dev/null`
    for i in ${JARFILES} ; do
	if [ -z "$LIBCLASSPATH" ] ; then
	    LIBCLASSPATH="$1/$i"
	else
	    LIBCLASSPATH="$LIBCLASSPATH:$1/$i"
	fi
    done
}

# Add the jar files in the Tweaker lib dir to the classpath.
add_to_libclasspath "${DACHMAESTRO_HOME}"/lib

# And finally, run ...
exec java \
    -classpath "$CLASSPATH:$LIBCLASSPATH" \
    -Dlog4j.configuration=file:"$DACHMAESTRO_HOME"/log4j.properties \
    -Xmx4500M \
     ibis.server.Server --stats --events --hub-only --port 5437
