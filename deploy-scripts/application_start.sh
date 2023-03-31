#!/bin/bash
. $(dirname $0)/constants.sh
cd $source_path/$DEPLOYMENT_GROUP_NAME

#wait until the application is stopped.
while true; do
  if [ $(systemctl is-active $application_name-$DEPLOYMENT_GROUP_NAME) == "failed" ] || [ $(systemctl is-active $application_name-$DEPLOYMENT_GROUP_NAME) == "inactive" ]; then
      break
  fi
  if [ $(systemctl is-active $application_name-$DEPLOYMENT_GROUP_NAME) == "active" ]; then
      sudo service $application_name-$DEPLOYMENT_GROUP_NAME stop
  fi
  sleep 1
done

#Start the application server.
sudo service $application_name-$DEPLOYMENT_GROUP_NAME start
exit 0
