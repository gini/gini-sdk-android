#! /bin/bash

if [[ $# < 1 ]]; then
   echo "doc dir required"
   exit 1
fi

DOC_DIR=$1
BUILD_DIR="$DOC_DIR/../../build"

mkdir $BUILD_DIR/integration-guide
cp -r $DOC_DIR/* $BUILD_DIR/integration-guide/
cd $BUILD_DIR/integration-guide
virtualenv ./virtualenv
source ./virtualenv/bin/activate
pip install -r requirements.txt
make html singlehtml
mkdir -p ../docs/
cp -r build/html/* ../docs/
rm -rf $BUILD_DIR/integration-guide


