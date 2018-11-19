#!/usr/bin/env bash

usage() {
  echo "Usage: update-version.sh -v [VERSION NUMBER]"
  echo "Required options:"
  echo "  -v    The version to update to"
  echo "  -f    The version to update from"
}

bsd_version() {
  # extract the bsd2017.version value from the parent pom file
  egrep -o '<bsd2017.version>.*</bsd2017.version>' pom.xml | sed -E 's/<\/?bsd2017.version>//g'
}

while getopts "f:v:h" opt; do
  case $opt in
    v)
      NEW_VERSION=$OPTARG
      echo "New BioSamples project version: $NEW_VERSION"
      ;;
    f)
      LAST_VERSION=$OPTARG
      echo "Old BioSamples project version: $LAST_VERSION"
      ;;
    h)
      usage
      exit 0
      ;;
    \?)
      usage
      exit 1
      ;;
    :)
      echo "Missing option argument for -$OPTARG"
      exit 1
      ;;
    *)
      echo "Unimplemented option: -$OPTARG"
      exit 1
      ;;
  esac
done

if [ -z "$NEW_VERSION" ] ;
then
  echo "No version number supplied - please give a non-empty version number to increment to"
  exit 1
fi

# Start setting maven versions arguments to what is going to be the new version

if [ -z "$LAST_VERSION" ] ;
then
  LAST_VERSION=$(bsd_version)
  echo "No last version number supplied - using the current version of the software $LAST_VERSION"
fi

# invoke maven versions plugin to increment project structure versions
mvn versions:set -DoldVersion="$LAST_VERSION" -DnewVersion="$NEW_VERSION"  || exit 1

# invoke maven versions plugin to update the bsd2017.version property
mvn versions:update-property -Dproperty=bsd2017.version -DoldVersion="$LAST_VERSION" -DnewVersion="$NEW_VERSION" || exit 1

# updates all the docker files and the shell scripts
echo " "
echo "Updating docker-compose and shell files to the new version"

find . -name "docker-compose.*yml" -or -name "*.sh" | xargs sed -i.versionsBackup "s/$LAST_VERSION/$NEW_VERSION/g" || exit 1

echo "Version update complete!"
