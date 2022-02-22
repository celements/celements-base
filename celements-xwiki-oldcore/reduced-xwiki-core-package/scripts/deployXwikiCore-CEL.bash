#!/bin/bash

pushd `dirname $0` > /dev/null
SCRIPTPATH=`pwd -P`
popd > /dev/null

printUsage()
{
        echo "Usage: `basename $0` <version-Num>"
        exit 1
}

if [ -z ${1} ];
then
  printUsage
fi

ver=CEL${1}
parentDir=`realpath ${SCRIPTPATH}/..`
newFolder=${parentDir}/${ver}
newName=xwiki-core-2.7.2-${ver}
newJarFile=${newFolder}/${newName}.jar

if [ -d ${newFolder} ];
then
  echo "new version folder is missing: ${newFolder}"
fi

groupId=com.xpn.xwiki.platform
artifactId=xwiki-core
repositoryId=ssh-external-repository
repoUrl=scpexe://maven.celements.ch/var/www-celements/externals
sourceFile=${parentDir}/orig/xwiki-core-2.7.2-sources.jar

mvn deploy:deploy-file -DgroupId=${groupId} \
  -DartifactId=${artifactId} \
  -Dversion=2.7.2-${ver} \
  -Dpackaging=jar \
  -Dfile=${newJarFile} \
  -DrepositoryId=${repositoryId} \
  -Durl=${repoUrl}

mvn deploy:deploy-file -DgroupId=${groupId} \
  -DartifactId=${artifactId} \
  -Dversion=2.7.2-${ver} \
  -Dpackaging=java-source \
  -Dfile=${sourceFile} \
  -DrepositoryId=${repositoryId} \
  -Durl=${repoUrl} \
  -DgeneratePom=false

