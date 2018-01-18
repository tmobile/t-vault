
![logo](T-Vault.png)

# T-Vault
T-Vault is built to simplify the process of secrets management. We wanted to build an intuitive and easy to use tool that application developers can easily adopt without sacrificing their agility while still following best practices for secrets management.
It uses a few open source products internally including, at its heart [Hashicorp Vault][1]. Hashicorp vault provides the core functionality of safely storing secrets at rest and access control to those secrets. T-Vault builds on that base to provide a higher-level of abstraction called Safe. Safes are logical abstractions, internally using the concept of paths within vault. 
T-Vault simplifies the access management to secrets by hiding away all the complexities of managing polices. 

A very intuitive web UI provides a nice layer of abstraction and hides all the complexities of managing paths, policies, token management, etc. T-Vault introduces two new personas, a 'Safe User' and 'Safe Administrator'. Safe admins will create Safes and grant access to individuals or a LDAP group or an application. Individuals with access to a Safe can use the web UI or API to do CRUD operations on secrets within their Safe.

When a Safe is created, T-Vault automatically creates the paths and boilerplate policies for that path. It also saves metadata about the Safe internally within the vault. Granting access to an individual for Safe involves associating user to the predefined policy for the path associated with the Safe. App roles and AWS App roles creation and granting access to them works the same way.

This readme file provides instructions to download, install, configure and use T-Vault API and user portal.

Here is a quick demo video.



<a href="http://www.youtube.com/watch?feature=player_embedded&v=fv3GOiFYAt8
" target="_blank"><img src="http://img.youtube.com/vi/fv3GOiFYAt8/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="240" height="180" border="10" /></a>



# Table of Contents

1. [Installation](#installation)
    * [Installation Prerequisites](#installation-prerequisites)
    * [How to install](#installation-steps)
2. [Configuration](#t-vault-configuration)
   * [Default Installation](#default-installation)
   * [Configuration Options](#t-vault-configuration-options)
3. [Install in Production](#install-in-production)
   * [Setup](#setup)
   * [High Availability](#high-availability)
   * [Un-Sealing](#un-sealing)
4. [License](#license)


# Installation

## Installation on Linux

### Installation-Prerequisites

Below are the dependencies required to build T-Vault from source.

* [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) - Required to compile/build java source code
* [Maven](https://maven.apache.org) - Required to build/package
* [Docker](https://www.docker.com/) - Required if docker based deployment is preferred
* [Node](https://nodejs.org/en/) and build tools (sudo yum install gcc-c++ make, sudo yum groupinstall 'Development Tools', bzip2)
* [Bower](https://bower.io/)
* [Gulp](https://gulpjs.com/)


### Installation Steps

You can build T-Vault from source using build_vault.sh. There are two packaging options available. The script can create a tar file or a docker image.
* In case you choose 'tar' option, please make sure all the development tools are installed on Linux. 
* For the case of docker image, make sure docker service is running locally.

#### Tar based installation

* Download source code (https://github.com/tmobile/t-vault).
* Go to the parent directory of the source code tree and run the command <pre>./build_tvault.sh --build all --package tar</pre>
* This will build both T-Vault API and user portal and generate tar file tvault_all.tar.gz
* Run the command <pre>./install_tvault.sh</pre>to install and start T-Vault and all of the dependent services.


#### Docker container based installation

[Docker](https://www.docker.com/) needs to be installed and running before performing this.

* Download source code (https://github.com/tmobile/t-vault).
* Go to the parent directory of the source code tree and run the command <pre>./build_tvault.sh --build all --package docker</pre>
* This will build both T-Vault API and user portal and push the docker image to local docker hub
* Run the command <pre>docker run --privileged -it -p 443:443 -p 8200:8200  your_tvault_docker_image_id /bin/bash</pre> to start cloud vault and all of the dependent services


### How to access T-Vault Services

After completing the installation, you can access

* The T-Vault User Portal using the URL https://your_ip_address
* The T-Vault API using the URL https://your_ip_address:8080
* The Vault Service using the URL https://your_ip_address:8200


# T-Vault Configuration

## Default Installation
Default installation, sets up vault with 
* AUTH BACKEND : Username Password
* STORAGE BACKEND : File System

`Default installation should be used only to test drive the tool, it should not be used in production environments. HA is not supported with the default installation.`

The default installation sets up few default users so that you can explore the tool right away.

1. safeadmin/safeadmin

   Safe Admin user. This user has all the privileges to create and manage safes. Post installation login with safeadmin to create safes and grant access to testuser1 & 2.  

2. vaultadmin/vaultadmin

   This is a Vault Admin user. By default this user is attached with policies to manage all the paths excepts for secret store mounts.  
   
 3. testuser1/testuser1, testuser2/testuser2
    
    These two testusers doesnt have any previleges by default. You can grant access to these users and try out the functionalities of T-Vault.  


## T-Vault Configuration Options
T-Vault supports Following Auth Backends and Storage Backends.

Auth Backends
```
* Username Password
* LDAP
* AWS Authentication
```

Storage Backends
```
* Consul
* File System
* Dynamo DB
```

You can configure your installation with combination of any of the Auth Backend and Storage Backend listed above.

The installation script requires the vault configuration information. These configurations are managed from the parameters file.
Sample T-Vault configuration parameters file.

```
###########################################################################
#                            Auth Backend                                 #
###########################################################################

# Allowed values for AUTH_BACKEND are userpass, ldap
AUTH_BACKEND=userpass
ENABLE_AWS=yes

###########################################################################
#                      LDAP Credentials                                   #
###########################################################################

#LDAP_URL='ldap://hostname.com:port'
#LDAP_GROUP_ATTR_NAME='cn'
#LDAP_USR_ATTR_NAME='---'
#USER_DN='---'
#GROUP_DN='----'
#BIND_DN='---'
#BIND_DN_PASS='---'
#TLS_ENABLED='false'
#VAULT_ADMIN_GROUP='---'
#SAFE_ADMIN_GROUP='---'

## The value for USE_UPNDOMAIN is either 'yes' or 'no'

#USE_UPNDOMAIN='yes'
#UPN_DOMAIN_URL='---'


##########################################################################
#                        Storage Backend                                 #
##########################################################################
# The possible values are 'File System' or 'Consul'
BACKEND='File System'

##########################################################################
#                         Consul Parameters                              #
##########################################################################

CONSUL_DATACENTER='dc1'
#CONSUL_ENCRYPT=''
CONSUL_RETRY_JOIN='127.0.0.1'
CONSUL_STORAGE_ADDRESS='127.0.0.1:8500'
CONSUL_STORAGE_PATH='tvault/tvault'
CONSUL_STORAGE_SERVICE_NAME='tvault'

##########################################################################
#                       Global                                           #
##########################################################################

SELF_SIGNED='y'

##########################################################################
#                         DynamoDB Parameters                            #
##########################################################################

#AWS_DYNAMODB_TABLE='tvault'
#AWS_DEFAULT_REGION='us-west-2'

```

# Install in Production
## Setup
For production installations use
```
1. AUTH BACKEND : LDAP
2. STORAGE BACKEND : Consul
```

Consul is the only storage backend that supports HA. When using LDAP as auth backend, you need to configure additional LDAP related parameters, two group names should be configured for setting up admin users for the T-Vault.

```
* SAFE_ADMIN_GROUP: All the members of this group will get safe admin privileges.
* VAULT_ADMIN_GROUP: All the members of this group will get vault admin privileges.
```

## High Availability
T-Vault has following components

|Component| Description|
|-----------------|-----------------------------------------------------------------------------------------|
|Nginx            |Hosts the UI, acts as a proxy for T-Vault API and Vault's native http rest interface.|
|Springboot App   |T-Vault API layer|
|Hashicorp Vault  |As is Hashicorp Vault|

Hashicorp Vault supports high availability with Consul as storage backend.


T-Vault UI, API and Vault is all bundled together into one unit. You can horizontally scale this unit and point it towards an HA Consul Cluster. If required you could split it up into multiple tiers and have load balancing and horizontal scaling for each of the layer.

Internally we have deployed our T-Vault on a container platform. The build scripts will create a readily usable tvault docker container (assuming with correct configuration file) which is then pushed to our container platform. We maintain a Consul cluster outside of the container platform.


## Un-sealing
One of the challenges with open source version of vault is, how to unseal a new instance of vault in case of HA setup. We have built an automated unsealing process using KMS and IAM Roles. You can come up with a solution that works for you. Same way the distribution of the master keys can be added to the installation script based on your preference.

For standalone installations on VMs, where automatic un-sealing is not preferred, T-Vault has web pages to un-seal vault instances manually(https://host:port/#/unseal). Users have to enter 3 out of 5 keys in the un-seal web form (or whatever the threshold that was used). Key holders can go to the unseal page and enter the IP address of the sealed vault and enter their master key to start unsealing.


# License

T-Vault is released under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0).


[1]: https://github.com/hashicorp/vault
