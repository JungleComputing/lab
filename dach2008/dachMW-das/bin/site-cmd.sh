#!/bin/bash

SITE=$1

shift

if [ -e ./bin/nodes/$SITE ]
then 
	echo Using site $SITE
	for i in `cat ./bin/nodes/$SITE` ; do  echo $i ; ssh -oConnectTimeout=10 -oStrictHostKeyChecking=no $i "$@" ; done
else
	echo Unknown site $SITE
fi

