#!/bin/bash
# This script will upload to Bintray. It is intended to be conditionally executed on tagged builds.

echo -e 'Bintray Upload Script => Branch ['$TRAVIS_BRANCH']  Tag ['$TRAVIS_TAG']'

if [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_TAG" != "" ]; then

  echo -e 'Bintray Upload => Decrypting keys ...\n'

  openssl aes-256-cbc -K $encrypted_506de5234d8d_key -iv $encrypted_506de5234d8d_iv -in key.private.F984A085.asc.enc -out key.private.F984A085.asc -d
  openssl aes-256-cbc -K $encrypted_506de5234d8d_key -iv $encrypted_506de5234d8d_iv -in key.public.F984A085.asc.enc -out key.public.F984A085.asc -d

  echo -e 'Bintray Upload => Starting upload ...\n'

  sbt storeBintrayCredentials

  sbt +publishSigned
  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo 'Completed upload!'
  else
    echo 'Upload failed.'
    exit 1
  fi
else
  echo 'Bintray Upload => Not a tagged build so will not upload'
fi
