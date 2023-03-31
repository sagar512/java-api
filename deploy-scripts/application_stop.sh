#!/bin/bash
. $(dirname $0)/constants.sh
cd $source_path/$DEPLOYMENT_GROUP_NAME
#Stop the application server.
sudo service $application_name-$DEPLOYMENT_GROUP_NAME stop
exit 0
