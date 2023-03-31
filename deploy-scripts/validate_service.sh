#!/bin/bash
#TODO: Team to optimize the time taken to start the server. Presently it takes about 245 seconds.
. $(dirname $0)/constants.sh
rm -rf $backup_path/$DEPLOYMENT_GROUP_NAME
rm -rf $code_deploy_path
i=0
while [ "$i" -lt 400 ]
do
  sleep 10
  pid=`lsof -t -i:$server_port`
  if [ "$pid" != "" ]
  then # Process is running
      echo "Process is running"
     exit 0
  fi
  i=$((i+10))
done

pid=`lsof -t -i:$server_port`
if [ "$pid" != "" ]
then # Process is running
    echo "Process is running"
   exit 0
else
	echo "Process does not exist(lsof -t -i:$server_port)"
        sudo service $application_name-$DEPLOYMENT_GROUP_NAME stop
  exit 1
fi
