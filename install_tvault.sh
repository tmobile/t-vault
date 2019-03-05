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
# =========================================================================

BASEDIR="$( cd "$(dirname "$0")" ; pwd -P )"
TVAULT_TAR_NAME=tvault_all.tar.gz

echo "----------------------------------------------------------------------"
echo "Installing the TVault application"
echo "----------------------------------------------------------------------"

cp $TVAULT_TAR_NAME /tmp
cd /tmp
tar -zxf $TVAULT_TAR_NAME

chmod +x /tmp/vault.sh
chmod +x /tmp/parameter

#./vault.sh -s silent
./vault.sh
