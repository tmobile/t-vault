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



FROM centos:7
RUN yum -y install vim tar gzip wget which sudo openssl initscripts java-1.8.0-openjdk glibc-common awscli && rm -rf /var/cache/yum/*
COPY vault_docker_install.sh /tmp/vault_install.sh
COPY vault.tar.gz /tmp/vault.tar.gz
COPY parameter /tmp/parameter
COPY vault.sh /tmp/vault.sh

RUN chmod +x /tmp/vault_install.sh
#RUN chmod +x /tmp/vault.sh
#RUN chmod +x /tmp/parameter
#RUN useradd -r -s /bin/false tvault
#RUN setfacl -m u:tvault:rwx /var/log
ENTRYPOINT /tmp/vault_install.sh ; /bin/bash
