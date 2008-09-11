#!/bin/bash

HOST=`/bin/hostname -f`

if [ -e ./bin/nodes/headnodes ]
then 
	echo Copying from site $HOST
	for i in `cat ./bin/nodes/headnodes` ; do 
		if [ $i == $HOST ] 
		then
			echo skipping $i
		else
			echo target site $i 
			scp -oConnectTimeout=10 -oStrictHostKeyChecking=no "$1" $i:
		fi
	done
else
	echo Headnodes not found!
fi

