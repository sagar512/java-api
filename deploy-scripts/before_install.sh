#!/bin/bash
. $(dirname $0)/constants.sh
#Make directories if they do not already exist.
mkdir -p  $code_deploy_path
mkdir -p  $backup_path/$DEPLOYMENT_GROUP_NAME
mkdir -p  $source_path
# Backup existing source files and remove the source files
cp -rp    $source_path/$DEPLOYMENT_GROUP_NAME $backup_path/$DEPLOYMENT_GROUP_NAME
rm -rf    $source_path/$DEPLOYMENT_GROUP_NAME
