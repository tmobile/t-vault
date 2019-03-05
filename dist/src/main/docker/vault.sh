#! /bin/bash
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

###############################################################################
# Author @vvaradh
# Date 08/10/2017
# Updated Date: 12/10/2017
# This script covers both interactive and silent mode installation. For silent
# mode we need to pass a flat file named parameters and specify it under 
# source part in the script
###############################################################################
source /tmp/parameter


#####################################################################################
#               Check availability of required parameters                           #
#####################################################################################

case $BACKEND in
  "Consul")
    if [[ -z $CONSUL_DATACENTER || -z $CONSUL_RETRY_JOIN ]]; then
      echo 'one or more missing variables in Consul parameters'
      exit 1
    fi
    ;;

  "AWS DynamoDB")
    if [[ -z $AWS_DYNAMODB_TABLE || -z $AWS_DEFAULT_REGION ]]; then
      echo 'one or more missing variables in DynamoDB parameters'
      exit 1
    fi
    ;;
  "File System")
    ;;    
  *) echo Invalid option in Backend;;
esac

case $AUTH_MODE in
     "LDAP")
     if [[ -z $LDAP_URL || -z $LDAP_GROUP_ATTR_NAME || -z $LDAP_USR_ATTR_NAME || -z $USER_DN || -z $GROUP_DN || -z $BIND_DN || -z $BIND_DN_PASS|| -z $TLS_ENABLED ]]; then
        echo 'one or more LDAP parameters are missing'
        exit 1
     fi
    ;;
esac

#####################################################################################
#                          Check for root user                                      #
#####################################################################################

if [[ $EUID -ne 0 ]]; then
  echo "This script must be run as root" 
  exit 1
fi

#####################################################################################
#        Basic Initialization/Settings needed for installation                      #
#####################################################################################

VHOME=""
echo "Creating the Vault installation directory..."

mkdir -p /opt/tvault

if [[ -d /opt/tvault ]]; then
	export VHOME="/opt/tvault"
else
	echo "Unable to create $VHOME. Installation failed."
	exit 1
fi

if [[ ! $(getent passwd tvault) ]] ; then
   echo "Creating the user tvault..."
   useradd tvault || true
fi
if [[ ! $(getent group tvault) ]]; then
   echo "Creating the group tvault..."
   groupadd tvault || true
   usermod -g tvault tvault
fi
#useradd tvault || true
#groupadd tvault || true
#usermod -g tvault tvault

VLOG="/var/log/app/"
chown -R tvault:tvault /var/log/tvault
mkdir -p $VLOG  || { echo "$VLOG folder creation failed" ; exit 1; }
echo "Creating the directory [$VLOG] to write application logs..."

mkdir /var/log/tvault
chown -R tvault:tvault /var/log/tvault
chmod -R  ugo+r $VLOG
VDOWNLOADS="$VHOME/tmp"
mkdir -p "$VDOWNLOADS"

INSTLOG="$VLOG/tvault-install.log"
echo "The installation logs will be available at [$INSTLOG]..."

##############################################################################
#       Copy the required Binaries and grant permissions                     #
##############################################################################

echo "Untaring $VDOWNLOADS/$VINST and installing Tvault in $VHOME..."
tar -xf /tmp/vault.tar.gz -C "$VHOME"
chown -R tvault:tvault $VHOME
chown -R tvault:tvault $VLOG


##############################################################################
#                               Utils/Helper Functions                       #
##############################################################################

# Heler to validate IP Address

function valid_ip()
{
  local  ip=$1
  local  stat=1

  if [[ $ip =~ ^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$ ]]; then
    OIFS=$IFS
    IFS='.'
    ip=($ip)
    IFS=$OIFS
    [[ ${ip[0]} -le 255 && ${ip[1]} -le 255 \
    && ${ip[2]} -le 255 && ${ip[3]} -le 255 ]]
    stat=$?
  fi
  return $stat
}

# Helper to extract and write the vault key
function getkey() 
{ 
  key="^Unseal Key $1"; 
  cat /opt/tvault/hcorp/vault.init  | grep "$key" | awk '{print $4}'  
}

# Helper to validate the inputs

function checkinput()
{
  if [[ ! -z "$1" ]]; then
    echo $1
  else
    echo $2
  fi
}

# Helpers to create config json for using consul and backend
function consulconf()
{
  CONSUL_CONF="$VHOME/consul/client/config.json"
  if [[ ! -f $CONSUL_CONF ]]; then
    echo "{" > $CONSUL_CONF
      if [[ -f $VHOME/consul/client/_config ]]; then
        cat $VHOME/consul/client/_config >> $CONSUL_CONF
      fi
  fi
  echo $1 >> $CONSUL_CONF
}

function createConsulConfigJson()
{

    CONSUL_DATACENTER=$CONSUL_DATACENTER
    CONSUL_ENCRYPT=$CONSUL_ENCRYPT
    CONSUL_RETRY_JOIN=$CONSUL_RETRY_JOIN

    export CONSUL_DATACENTER=$CONSUL_DATACENTER; echo "[$CONSUL_DATACENTER]"; consulconf "\"datacenter\":\"$CONSUL_DATACENTER\",";

    if [[ ! -z $CONSUL_ENCRYPT  ]]; then
      export CONSUL_ENCRYPT=$CONSUL_ENCRYPT; echo "[$CONSUL_ENCRYPT]"; consulconf "\"encrypt\":\"$CONSUL_ENCRYPT\",";
    fi

    export CONSUL_RETRY_JOIN=$CONSUL_RETRY_JOIN; echo "[$CONSUL_RETRY_JOIN]"; consulconf "\"retry_join\":[\"$CONSUL_RETRY_JOIN\"]";

    consulconf "}"

}

# Helper to generate keys
function genselfcert()
{
  if ! type "openssl" > /dev/null; then
    return 1
  fi

  IP=$(hostname -I | cut -d' ' -f1)

  if valid_ip $IP; then 
    echo "IP.1 = $IP" >> $2
  else 
    echo "Unable to determine IP address. Exiting ..."
  exit 1
  fi

  cert_pass=$3
  openssl req -x509 -batch -nodes -newkey rsa:2048 -keyout $1/tvault.key -out $1/tvault.crt -config $2 -days 9999
  openssl pkcs12 -export -in $1/tvault.crt -inkey $1/tvault.key -out $1/tvault.p12 -name self -passout pass:$cert_pass
}
SSCRED_FILE_LOCATION="/opt/tvault/hcorp"
##############################################################################
# End - Utils
##############################################################################



##############################################################################
# Collect installation parameters
##############################################################################

VCONF="$VHOME/hcorp/conf/vault.conf"

IP=$(hostname -I | cut -d' ' -f1)

if valid_ip $IP; then 
  echo "IP address is : $IP"
else 
  echo "Unable to determine IP address. Exiting ..."
exit 1
fi

VSERVERCONF=$VHOME/hcorp/conf/server.hcl

if [[ $HOST == "" ]]; then
   redirect_addr="https://"$IP":8200"
else 
   redirect_addr="https://"$HOST":$PORT1"
fi

export VAULT_REDIRECT_ADDR=$redirect_addr

if [[ -f $VCONF ]]; then
  mv -f $VCONF "$VCONF.old"
  touch $VCONF
fi

echo "The Storage Backend chosen is [$BACKEND]..."

case $BACKEND in
    "File System")
      echo "Configuring the Backend [File System]..."
      echo "vserverconf=$VHOME/hcorp/conf/server.hcl" >> $VCONF
      #break
      ;;

    "AWS DynamoDB")
      echo "Configuring the Backend [AWS DynamoDB]..."
      export AWS_DYNAMODB_TABLE=$AWS_DYNAMODB_TABLE ; echo "[$AWS_DYNAMODB_TABLE]"
      export AWS_DEFAULT_REGION=$AWS_DEFAULT_REGION; echo "[$AWS_DEFAULT_REGION]"
      VSERVERCONF=$VHOME/hcorp/conf/server_dynamodb.hcl
      echo "vserverconf=$VHOME/hcorp/conf/server_dynamodb.hcl" >> $VCONF
      echo "AWS_DYNAMODB_TABLE=$AWS_DYNAMODB_TABLE" >> $VCONF
      #break
      ;;

    "Consul")
      echo "Configuring the Backend [Consul]..."
      createConsulConfigJson
      VSERVERCONF="$VHOME/hcorp/conf/server_consul.hcl"
      echo "vserverconf=$VHOME/hcorp/conf/server_consul.hcl" >> $VCONF
	  sed -i 's/CONSUL_STORAGE_ADDRESS/'"$CONSUL_STORAGE_ADDRESS"'/; s#CONSUL_STORAGE_PATH#'"$CONSUL_STORAGE_PATH"'#; s/CONSUL_STORAGE_SERVICE_NAME/'"$CONSUL_STORAGE_SERVICE_NAME"'/;' $VHOME/hcorp/conf/server_consul.hcl
      #break         
      ;;

    *) echo Invalid option;;
esac

##############################################################################
#                    Generate Certificates...                                #
##############################################################################

_use_selfsigned=$SELF_SIGNED
CERT_PASSWORD=""

if [[ "$_use_selfsigned" == "n" ]]; then
  echo " => Copy the tvault.crt, tvault.key and tvault.p12 file to $VHOME/certs"
  echo -n " => Enter the password for the PKCS12 keystore:"; read CERT_PASSWORD;

  if [[ ! -f $VHOME/certs/tvault.crt ||  ! -f $VHOME/certs/tvault.key  || ! -f $VHOME/certs/keystore.p12 ]]; then
    echo "Certificate file not found in $VHOME/certs. Exiting ..."
    exit 1
  else
  echo "Certificate files found."
  fi

else
  echo "Generating self signed certificates..."
  CERT_PASSWORD=$(head /dev/urandom | tr -dc A-Za-z0-9 | head -c 14)
  genselfcert $VHOME/certs $VHOME/certs/template.cfr $CERT_PASSWORD
  chown -R tvault:tvault $VHOME/certs
  export VAULT_SKIP_VERIFY=1
  echo "VAULT_SKIP_VERIFY=1" >> $VCONF
fi

ELB_ADD=$ELB_VALUE
if [ -n "$_redirect_addr" ]; then
   export VAULT_REDIRECT_ADDR=$_redirect_addr
fi
echo "The Vault Redirect address is [$VAULT_REDIRECT_ADDR]..."
echo "VAULT_REDIRECT_ADDR=$VAULT_REDIRECT_ADDR" >> $VCONF

##############################################################################
# End - Collect installation parameters
##############################################################################


################################################################################
# Vault start and init
################################################################################

chown tvault:tvault $VHOME/hcorp/conf/vault.conf
chmod +x $VHOME/hcorp/bin/*
ln -sf $VHOME/hcorp/bin/tvault /etc/init.d/tvault

export PATH=$PATH:$VHOME/hcorp/bin:$VHOME/api/bin
echo "Path is $PATH"
export VAULT_ADDR="https://127.0.0.1:8200"

#sudo setcap cap_ipc_lock=+ep $(readlink -f $(which vault)) 
sudo setcap cap_ipc_lock=+ep $VHOME/hcorp/bin/vault
#vault server -config=$VSERVERCONF >> $VLOG/tvault-vault-server.log &

echo "Vault server starting... "
service tvault start >> $INSTLOG


sleep 10s

n=0

until [ $n -ge 10 ]
do
  initstat=$(curl -sfk https://127.0.0.1:8200/v1/sys/init)
  rval=$?

    if [[ "$rval" == "0" ]]; then
      break
    else
      echo "Retrying \"https://127.0.0.1:8200/v1/sys/init\"  ...."
    fi
  n=$[$n+1]
  sleep 5
done

initstat=$(curl -sfk https://127.0.0.1:8200/v1/sys/init)

if [ $? -gt 0 ]; then
  echo "Vault service is not up, exiting."
  exit 1
fi

initstat=$(curl -sfk https://127.0.0.1:8200/v1/sys/init | grep "true")
echo "Initstatus: $initstat"


if [[ -z "$initstat" ]]; then

  echo "Initializing Vault..."
  echo "This only happens once when the server is started against a new backend that has never been used with Vault before."
  echo "During initialization, the encryption keys are generated and 5 unseal keys are created."

  vault operator init 1> $VHOME/hcorp/vault.init 2>> $INSTLOG

  sleep 2
  echo "Unsealing Vault"
  vault operator  unseal  $(getkey 1) >> $INSTLOG
  vault operator  unseal  $(getkey 2) >> $INSTLOG
  vault operator  unseal  $(getkey 3) >> $INSTLOG

################################################################################
# Vault Configuration
################################################################################

#Wait for unseal to complete
  n=0
  sealed=0
  until [ $n -ge 3 ]

  do
    SEAL_STATUS=$(service tvault seal-status)
    if [[ "$SEAL_STATUS" == "false" ]]; then
      sealed=1
      break
  else
    echo "Vault is in sealed state. Retrying ..."
  fi
  
  n=$[$n+1]
  sleep 5
  done

  if [ $sealed -eq 0 ]; then
    echo "Unable to unseal vault. Exiting installation."
   exit 1
  fi

  sleep 5s
  roottoken=$(cat $VHOME/hcorp/vault.init | grep '^Initial Root' | awk '{print $4}')

  vault login $roottoken >> $INSTLOG

  echo "Enabling Vault Audit..."
  vault audit enable file file_path=$VLOG/tvault-vault_audit.log >> $INSTLOG

  echo "Adding Mount paths..."
  vault secrets enable -path=apps generic >> $INSTLOG
  vault secrets enable -path=users generic >> $INSTLOG
  vault secrets enable -path=shared generic >> $INSTLOG
  vault secrets enable -path=metadata generic >> $INSTLOG

  echo "Configuring the Authentication Backend [$AUTH_BACKEND]..." 
  if [[ "$AUTH_BACKEND" == "ldap" ]]; then
     # LDAP...
     echo "Enabling/Configuring LDAP auth..."
     vault auth enable ldap >> $INSTLOG
     vault secrets tune -default-lease-ttl=30m /auth/ldap >> $INSTLOG
     if [[ "$USE_UPNDOMAIN" == "yes" ]]; then
        echo "Using UPN Domain:"
        vault write auth/ldap/config url=$LDAP_URL  groupattr=$LDAP_GROUP_ATTR_NAME userattr=$LDAP_USR_ATTR_NAME  userdn=$USER_DN   groupdn=$GROUP_DN   insecure_tls=true starttls=$TLS_ENABLED upndomain=$UPN_DOMAIN_URL >> $INSTLOG
     else 
        vault write auth/ldap/config url=$LDAP_URL  groupattr=$LDAP_GROUP_ATTR_NAME userattr=$LDAP_USR_ATTR_NAME  userdn=$USER_DN   groupdn=$GROUP_DN   binddn="$BIND_DN" bindpass="$BIND_DN_PASS" insecure_tls=true starttls=$TLS_ENABLED >> $INSTLOG
     fi

     vault policy write safeadmin $VHOME/hcorp/conf/safeadmin.json >> $INSTLOG
     vault policy write vaultadmin $VHOME/hcorp/conf/vaultadmin.json >> $INSTLOG
     vault write auth/ldap/groups/$VAULT_ADMIN_GROUP policies=vaultadmin >> $INSTLOG
     vault write auth/ldap/groups/$SAFE_ADMIN_GROUP policies=safeadmin >> $INSTLOG

  elif [[ "$AUTH_BACKEND" == "userpass" ]]; then
     # userpass...

     echo "Enabling userpass auth..."
     vault auth enable userpass >> $INSTLOG
     vault secrets tune -default-lease-ttl=15m /auth/userpass >> $INSTLOG

     echo "Writing Policiesfor userpass auth..."
     vault policy write safeadmin $VHOME/hcorp/conf/safeadmin.json >> $INSTLOG
     vault policy write vaultadmin $VHOME/hcorp/conf/vaultadmin.json >> $INSTLOG

     echo "Assigning policies for safeadmin, vaultadmin for userpass auth..."
     vault write auth/userpass/users/safeadmin password=safeadmin policies=safeadmin
     vault write auth/userpass/users/vaultadmin password=vaultadmin policies=vaultadmin

     echo "Creating test users..."
     vault write auth/userpass/users/testuser1 password=testuser1 policies=default
     vault write auth/userpass/users/testuser2 password=testuser2 policies=default
  fi


  if [[ "$ENABLE_AWS" == "yes" ]]; then
     echo "Enabling AWS auth..."
     vault auth enable aws >> $INSTLOG
     vault secrets tune -default-lease-ttl=15m /auth/aws >> $INSTLOG
  fi

################################################################################
# End - Vault Configuration                                               
################################################################################

else
  echo "Vault is already initialized, it should be unsealed using the unseal keys."
fi

echo "Vault setup complete."

echo "############################# Vault Status ######################################"
vault status
echo "#################################################################################"

################################################################################
# End - Vault start and init
################################################################################


################################################################################
# API start
################################################################################

echo "Setting up web application"

API_CONF="$VHOME/api/bin/tvaultapi.conf"
touch API_CONF
echo "JAVA_OPTS=\"-DTVAULT-API-LOG-PATH=$VLOG/\"" >> $API_CONF
echo "LOG_FOLDER=$VLOG" >> $API_CONF
echo "RUN_ARGS=\"--vault.api.url=https://127.0.0.1:8200/v1 --selfservice.ssfilelocation=$SSCRED_FILE_LOCATION --vault.port=8200 --vault.auth.method=$AUTH_BACKEND --vault.ssl.verify=false --server.port=8443 --server.ssl.key-store=/opt/tvault/certs/tvault.p12 --server.ssl.keyStoreType=PKCS12 --server.ssl.key-store-password=$CERT_PASSWORD\"" >> $API_CONF

chmod +x $VHOME/api/bin/tvaultapi.jar
chmod +x $VHOME/web/nginx/sbin/nginx
chmod +x $VHOME/web/bin/tnginx

ln -sf $VHOME/web/bin/tnginx /etc/init.d/tnginx
ln -sf $VHOME/api/bin/tvaultapi.jar /etc/init.d/tvaultapi

echo "Starting api service ... "
systemctl enable tvaultapi.service
service tvaultapi start

################################################################################
# End API start
###############################################################################

################################################################################
# Removing Vault Init
################################################################################

FILE="/opt/tvault/hcorp/vault.init"

################################################################################
# Startup - Adding services on startup
################################################################################

chkconfig tvault on
chkconfig tvaultapi on
chkconfig tnginx on

################################################################################
# Nginx start
################################################################################

echo "Starting web server ... "
systemctl enable tnginx.service
service tnginx start 

################################################################################
# End - Nginx start
################################################################################
