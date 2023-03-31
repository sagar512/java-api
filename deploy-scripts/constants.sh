#!/bin/bash
# Application constants
application_name="people-backend" # application-name specified in build.sbt
code_deploy_path="/home/ubuntu/$application_name-code-deploy-source"
source_path="/home/ubuntu/$application_name-source"
backup_path="/home/ubuntu/$application_name-backup"
jar_path="$source_path/$DEPLOYMENT_GROUP_NAME/target/people-backend.jar"
#---------------------------------------------------------------#
case $DEPLOYMENT_GROUP_NAME in
  dev)
    server_port=7300
      ;;
  integration)
    server_port=8300
      ;;
  staging)
    server_port=9300
      ;;
  production)
    server_port=7300
      ;;
  pre-production)
    server_port=7301
      ;;
  preprod_CR)
    server_port=7301
      ;;
esac
echo $server_port
