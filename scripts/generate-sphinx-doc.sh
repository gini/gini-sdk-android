#! /bin/bash

DOC_DIR=ginisdk/src/doc
BUILD_DIR=ginisdk/build

mkdir -p $BUILD_DIR/integration-guide
cp -r $DOC_DIR/* $BUILD_DIR/integration-guide/
cd $BUILD_DIR/integration-guide
virtualenv ./virtualenv
source ./virtualenv/bin/activate
pip install -r requirements.txt
make html singlehtml
mkdir -p ../docs/
cp -r build/html/* ../docs/
deactivate
cd ..
rm -rf integration-guide/
