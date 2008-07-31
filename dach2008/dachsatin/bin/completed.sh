#!/bin/sh
while true; do echo `date` : `grep Complete output/* | wc -l`; sleep 3; done
