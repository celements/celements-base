#!/bin/bash

pushd `dirname $0` > /dev/null
SCRIPTPATH=`pwd -P`
popd > /dev/null

parentDir=`realpath ${SCRIPTPATH}/..`
removeClasses=${parentDir}/classesToRemove.txt
ver=CEL${1}
newFolder=${parentDir}/${ver}
newName=xwiki-core-2.7.2-${ver}
newJarFile=${newFolder}/${newName}.jar

if [ -d ${newFolder} ];
then
  echo "version: ${ver} already exists"
  echo "exiting."
  exit
fi

mkdir ${newFolder}
cp ${parentDir}/orig/xwiki-core-2.7.2.jar ${newJarFile}

jar -tf ${newJarFile} \
| fgrep -f ${removeClasses} \
| awk -v newJarFile=${newJarFile} '\
 BEGIN{ printf("zip -d %s", newJarFile); } \
 { printf(" %s", $0); }'\
| bash

