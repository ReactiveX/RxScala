#!/bin/bash
# This script will upload to Bintray. It is intended to be conditionally executed on tagged builds.

echo -e 'Bintray Upload Script => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG'] Scala ['$TRAVIS_SCALA_VERSION']'

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then

  echo -e 'Bintray Upload => Starting upload ...\n'

  sbt storeBintrayCredentials

  sbt ++$TRAVIS_SCALA_VERSION publish
  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo 'Completed upload!'
  else
    echo 'Upload failed.'
    exit 1
  fi

  # snyc to Sonatype
  export SONA_USER=$sonatypeUsername
  export SONA_PASS=$sonatypePassword
  sbt ++$TRAVIS_SCALA_VERSION bintray::syncMavenCentral
  RETVAL=$?
  if [ $RETVAL -eq 0 ]; then
    echo 'Completed sync to Sonatype!'
  else
    echo 'Sync to Sonatype failed.'
    exit 2
  fi

else
  echo 'Bintray Upload => Not a tagged build so will not upload'
fi
