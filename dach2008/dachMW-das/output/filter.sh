#!/bin/sh
#egrep "Got steal reply: |Completed" $1 | sed s/Connection\ Handler/ConnectionHandler/ | awk '{ print $1 " " $6 }' | sed s/Got/Steal/
egrep "Got steal reply| Completed|steal request" $1 | sed s/Connection\ Handler/ConnectionHandler/ | sed s/ms./\ ms./ | grep -v NOT
