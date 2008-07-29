#!/bin/bash

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

# TODO check if these are OK on all clusters ?
export JAVA_HOME=/usr/local/jdk
export PATH=/data/local/gfarm_v2/bin:$JAVA_HOME/bin:$PATH

# TODO set this one from the deployment app ?
export DACHSATIN_HOME=/home/dach004/dachsatin-0.8

# Add the jar files in the VideoPlayer lib dir to the classpath.
add_to_libclasspath "${DACHSATIN_HOME}"/lib

# TODO move these to system properties
export DACH_EXECUTABLE=/home/dach/finder/dach.sh
export DACH_COPY=/bin/cp

HOST=`/bin/hostname -f`
PID=$$
CORES=`cat /proc/cpuinfo | grep bogomips | wc -l`
DIR=`mktemp -d`

echo Running on $HOST
echo Available cores $CORES
echo Using tmp dir $DIR
echo Mounting DFS on $DIR/dfs

mkdir -p $DIR/dfs
mkdir -p $DIR/tmp

gfarm2fs $DIR/dfs

if [ -d $DIR/dfs/problems ];
then
echo "$DIR/dfs mounted succesfully"
fi

# Split the command line options in normal 
# and special (-D) parameters
NORMAL=
SPECIAL=

while [ $# -gt 0 ]
do

        HEADER=`echo $1 | sed 's/\(..\).*/\1/'`

        case "$HEADER" in
        -D)
                SPECIAL="$SPECIAL $1"
                ;;
        *)
                NORMAL="$NORMAL $1"
                ;;
        esac
        shift
done

for c in `seq $CORES`
do
	echo Starting Satin $c with parameters $SPECIAL \
            -Ddach.machine.id=$HOST.$PID.$c \
            -Ddach.dir.tmp=$DIR/tmp \
            -Ddach.dir.data=$DIR/data \
            $JAVA_HOME/bin/java $NORMAL 
done


# And finally, run ...
#    -Dsmartsockets.file="$DACHSATIN_HOME"/smartsockets.properties \
#$JAVA_HOME/bin/java \
#    -classpath "$CLASSPATH:$LIBCLASSPATH" \
#    -Dlog4j.configuration=file:"$DACHSATIN_HOME"/log4j.properties \
#    -Dibis.server.address=$SERVERHOST:5437 \
#    -Dibis.pool.name=DACH -Dsatin.detailedStats \
#     ibis.dachsatin.Main "$@"

echo Unmounting DFS

fusermount -u $DIR/dfs

rm -r $DIR






