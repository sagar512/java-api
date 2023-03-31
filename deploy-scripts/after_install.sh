#!/bin/bash
. $(dirname $0)/constants.sh
#Copy source to our depoyment group folder.
cp -rp $code_deploy_path $source_path/$DEPLOYMENT_GROUP_NAME
cd $source_path/$DEPLOYMENT_GROUP_NAME
#sudo forever-service install $application_name-$DEPLOYMENT_GROUP_NAME --script server.js
if [ -e $jar_path ] #compiled jar of the source exists in the deployment.
then
    echo "We have the Jar file for deployment at $jar_path" 
else
    mvn package -Dmaven.test.skip=true
fi
#install application as service.
