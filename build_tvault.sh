#!/bin/bash
#set -x
# =========================================================================
# Copyright 2019 T-Mobile, US
# 
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# See the readme.txt file for additional language around disclaimer of warranties.
# ========================================================================

function exists {
   found=1
   type $1 >/dev/null 2>&1 || { found=0; }
   echo "$found"
}

APPS_TO_BUILD=NONE
PKG_TYPE=NONE

function usage {
   if [ $# -lt 4 ]; then
      echo "Usage: `basename $0` [--build|-b ui|api|all|none] [--package|-p tar|docker|none]"
      echo ""
      echo "Options: "
      echo ""
      echo "--build, -b             Builds all the cloud vault applications"
      echo "            ui          builds the cloud vault user portal"
      echo "            api         builds the cloud vault api"
      echo "            all         builds the cloud vault user portal and api"
      echo "            none        does not build any of the cloud vault applications"
      echo ""
      echo "--package, -p               Packages all the cloud vault application"
      echo "            tar         packages the cloud vault application as tar file"
      echo "            docker      packages the cloud vault application as docker container"
      echo "            none        does not package any of the cloud vault applications"
      echo ""
   fi
}

argc=$#
argv=($@)

for (( i=0; i<argc; i++ )); do
   option=${argv[i]}
   #echo "$option"
   j=i+1
   optionval=${argv[j]}
   #echo "optionval $optionval"
   #echo "option $option"
   case "$option" in
      --build|-b)
	# echo "build option"
         #optionval=${argv[i+1]}
         if [[ "$optionval" ==  "ui" ]]; then
            APPS_TO_BUILD=UI_ONLY
         elif [[ "$optionval" ==  "api" ]]; then
            APPS_TO_BUILD=API_ONLY
         elif [[ "$optionval" ==  "all" ]]; then
            APPS_TO_BUILD=ALL
         elif [[ "$optionval" ==  "none" ]]; then
            APPS_TO_BUILD=NONE
         else
            echo "Invalid build option specified"
         fi
      ;;
       --package|-p)
      # echo "package option $optionval"
         if [[ "$optionval" ==  "docker" ]]; then
            PKG_TYPE=docker
         elif [[ "$optionval" ==  "tar" ]]; then
            PKG_TYPE=tar
         elif [[ "$optionval" ==  "none" ]]; then
            PKG_TYPE=NONE
         else
            echo "Invalid package option specified"
            # As of now default this to tar
            PKG_TYPE=tar
         fi
      ;;
      --usage|-u|--help|-h)
      usage
      exit 0;
  esac
done

if [ $# -lt 4 ]; then
   echo "Insufficent build/package options provided. Pre-built applications will be used and packaged as tar."
   usage
fi

echo "Cleaning the previously generated files/directories..."

if [ -z "out_bin/" ]; then
   echo "Removing out/bin ..."
   rm -rf out_bin/
fi
if [ -z "tvault_final/" ]; then
   echo "Removing tvault_final/ ..."
   rm -rf tvault_final/
fi
if [ -f "tvault_all.tar.gz" ]; then
   echo "Removing tvault_all.tar.gz ..."
   rm -f tvault_all.tar.gz
fi

echo "Building $APPS_TO_BUILD and packaging as $PKG_TYPE"

BASEDIR="$( cd "$(dirname "$0")" ; pwd -P )"
COMPONENTS_DIR=$BASEDIR/dist/src/main/components
DOCKER_FILE_DIR=$BASEDIR/dist/src/main/docker
TVAULT_TAR_DIR=$BASEDIR/tvault_final

if [[ "$APPS_TO_BUILD" == "UI_ONLY"  || "$APPS_TO_BUILD" == "ALL" ]]; then
   echo "-----------------------------------------------------"
   echo "Building TVault User Portal"
   echo "-----------------------------------------------------"

   # check for nodejs
   # check for gulp
   # check for Bower
   if [ "$(exists node)" == "0" ] ; then
      echo "Node is not installed/not available in the path. Please correct it and try again later"
      exit 1  
   fi
   if [ "$(exists bower)" == "0" ] ; then
      echo "Bower is not installed/not available in the path. Please correct it and try again later"
      exit 1  
   fi
   if [ "$(exists gulp)" == "0" ] ; then
      echo "Gulp is not installed/not available in the path. Please correct it and try again later"
      exit 1  
   fi
   
   #UI_DIR=$BASEDIR/tvaultui
   #cd $UI_DIR
   #echo "Clean up existing node_modules directory..."
   #echo "Removing $UI_DIR/node_modules/ ..."
   #rm -rf node_modules
   #echo "Removing $UI_DIR/.tmp ..."
   #rm -rf .tmp
   #echo "Removing $UI_DIR/app/ ..."
   #rm -rf app
   #echo "Completed removing the existing node_modules directory..."

   #npm install
   #bower install --allow-root
   #gulp serve:live
   #gulp build

   #New UI build

   UI_DIR=$BASEDIR/tvaultuiv2
   cd $UI_DIR
   echo "Clean up existing node_modules directory..."
   echo "Removing $UI_DIR/node_modules/ ..."
   rm -rf node_modules
   rm -rf build
   echo "Completed removing the existing node_modules and build directory..."

   npm install
   npm run build

   UI_DESTINATION_DIR=$COMPONENTS_DIR/web/nginx/html
   echo "Copying Vault UI to $UI_DESTINATION_DIR"
   rm -rf $COMPONENTS_DIR/web/nginx/html/*
   cp -rf $UI_DIR/build/* $UI_DESTINATION_DIR/.

   #mkdir -p $UI_DESTINATION_DIR/styles/sass/components/
   #cp $UI_DIR/src/app/sass/components/jquery-ui.css $UI_DESTINATION_DIR/styles/sass/components/
   if [[ "$PKG_TYPE" == "NONE" ]]; then
      exit 0;
   fi
fi

if [[  "$APPS_TO_BUILD" == "API_ONLY" || "$APPS_TO_BUILD" == "ALL" ]]; then
   echo "-----------------------------------------------------"
   echo "Building TVault API"
   echo "-----------------------------------------------------"
   # Check for JAVA
   if [ "$(exists java)" == "0" ] ; then
      echo "Java is not installed/not available in the path. Please correct it and try again later"
      exit 1  
   fi
   if [ "$(exists mvn)" == "0" ] ; then
      echo "Maven is not installed/not available in the path. Please correct it and try again later"
      exit 1  
   fi

   API_DIR=$BASEDIR/tvaultapi
   cd $API_DIR
   mvn clean package
   cd $BASEDIR
   API_DESTINATION_DIR=$COMPONENTS_DIR/api/bin
   mkdir -p $API_DESTINATION_DIR

   cp -f $API_DIR/target/tvaultapi.jar $API_DESTINATION_DIR/tvaultapi.jar
   if [[ "$PKG_TYPE" == "NONE" ]]; then
      exit 0;
   fi
fi

cd $BASEDIR
if [[ "$PKG_TYPE" == "tar"  || "$PKG_TYPE" == "docker" ]]; then
   echo "-----------------------------------------------------"
   echo "Creating tar"
   echo "-----------------------------------------------------"
   cd $BASEDIR
   
   echo "Extracting vault.zip"
   unzip -o $COMPONENTS_DIR/hcorp/bin/vault.zip -d $COMPONENTS_DIR/hcorp/bin/
   rm -f $COMPONENTS_DIR/hcorp/bin/vault.zip
   
   mkdir -p $TVAULT_TAR_DIR
   cp -rf $COMPONENTS_DIR/ $TVAULT_TAR_DIR/

   cd $TVAULT_TAR_DIR/components
   
   #cp -rf $DOCKER_FILE_DIR/vault.sh $TVAULT_TAR_DIR
   #cp -rf $DOCKER_FILE_DIR/parameter $TVAULT_TAR_DIR

   tar -zcf ../../vault.tar.gz *

   #cp -rf $DOCKER_FILE_DIR/vault.sh $TVAULT_TAR_DIR
   #cp -rf $DOCKER_FILE_DIR/parameter $TVAULT_TAR_DIR

   cd $BASEDIR
   mkdir -p out_bin

   mv -f vault.tar.gz out_bin/
   cp -rf $DOCKER_FILE_DIR/vault.sh out_bin/
   cp -rf $DOCKER_FILE_DIR/parameter out_bin/

   cd out_bin
   echo "Creating tvault_all.tar.gz"
   tar -zcf ../tvault_all.tar.gz *

fi
if [[ "$PKG_TYPE" == "docker" ]]; then
   echo "-----------------------------------------------------"
   echo "Creating Docker Container"
   echo "-----------------------------------------------------"
   cp $BASEDIR/tvault_all.tar.gz $DOCKER_FILE_DIR/
   cd $BASEDIR   
fi 

echo "-----------------------------------------------------"
echo "Completed Successfully"
echo "-----------------------------------------------------"
