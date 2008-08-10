#!/bin/sh

#SERVERHOST=babylon.few.vu.nl
#SERVERHOST=fs0.das3.cs.vu.nl
SERVERHOST=hongo001.logos.ic.i.u-tokyo.ac.jp

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

# Add the jar files in the Maestro lib dir to the classpath.
add_to_libclasspath "${DACHMAESTRO_HOME}"/lib

# And finally, run ...
#    -Dsmartsockets.file="$DACHMAESTRO_HOME"/smartsockets.properties \
exec $JAVA \
    -server \
    -classpath "$CLASSPATH:$LIBCLASSPATH" \
    -Dlog4j.configuration=file:"$DACHMAESTRO_HOME"/log4j.properties \
    -Dibis.server.address=$SERVERHOST:5437 \
    -Dhardwarename=`uname -m` \
    -Xmx800M \
     ibis.maestro.MasterWorkerProgram "$@"
