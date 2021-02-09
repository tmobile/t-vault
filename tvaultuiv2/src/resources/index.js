const Resources = {
  tvaultDescription:
    'T-Vault is a simplified and centralized secrets management solution for securely storing, distributing and tightly controlling access to passwords, certificates, encryption keys, tokens for protecting secrets and other sensitive data using a UI, HTTP API.',
  loginNotes:
    ' This instance of T-Vault is for the Cloud Security Team to share AD Service account passwords, SSH keys, AWS Access Keys, Certificates and other general sensitive information to Cloud Customers only. Do not use it for Tier1 application integrations.',
  storeDescription:
    'Centrally store, access, and distribute secrets like API keys, AWS IAM/STS credentials, SQL/NoSQL databases, X.509 certificates, SSH credentials, and more.',
  accessDescription:
    'Secure and tight access control for accessing passwords, certificates, encryption keys for protecting secrets and other sensitive data using a UI and HTTP API.',
  distributeDescription:
    'Easily create safes, add secrets and share access to others. Integrate T-Vault with your existing workflows to distribute secrets to users.',
  serviceAccount:
    "Service Account Activation. By default passwords are not set to autorotate <br/> <p>Note: When 'Enable Password Rotation' is turned off, the password for this service account will not be autorotated by T-Vault.</p>",
  offBoardConfirmation:
    'Are you sure you want to offboard this Service Account? This will not delete the Service Account from AD server.',
  offBoardSuccessfull:
    'Offboarding of Service Account has been completed successfully. The Service Account Password can no longer be managed by T-Vault. For security reasons, you need to log out and log in order for the changes to effect.',
  svcNotEnableUpdateMsg:
    'days and will not be enabled for auto rotation by T-Vault even after it is expired. You need to make sure the password for this service account is getting rotated appropriately.',
  svcNotEnableOnboardMsg:
    'days and will not be enabled for auto rotation by T-Vault. You need to make sure the password for this service account is getting rotated appropriately.',
  svcPwdEnableNoValueMsg:
    'days and enabled for auto rotation by T-Vault. When you request for the password after this time, T-Vault will generate new password and make it available.',
  svcPwdEnableWithValueMsg:
    'seconds and enabled for auto rotation by T-Vault. When you request for the password after this time, T-Vault will generate new password and make it available.',
  noSafeSecretFound:
    'Add a <strong>Folder</strong> and then you will be able to add <strong>Secrets</strong> to view them all here.',
  noSafeSecretFoundReadPerm:
    'No <strong>Folder</strong> or <strong>Secrets</strong> found here.',
  noUsersPermissionFound:
    'No <strong>Users</strong> are given permission to access this safe, add users to access the safe.',
  noGroupsPermissionFound:
    'No <strong>Groups</strong> are given permission to access this safe, add groups to access the safe.',
  noAwsPermissionFound:
    'No <strong>Applications</strong> are given permission to access this safe, add applications to access the safe.',
  noAppRolePermissionFound:
    'No <strong>App roles</strong> are given permission to access this safe, add approles to access the safe.',
  transferConfirmation:
    'Are you sure you want to transfer service account owner?',
  noCertificatesFound:
    'Once you add a <strong>Certificate</strong> you’ll see the  Corresponding <strong>Details</strong> here!',
  appRoles:
    'AppRoles operate a lot like safes, but they put the application as the logical unit for sharing. Additional Accessor ID and Secret ID pairs can easily be created through T-Vault, Secret IDs can only be accessed when downloaded.',
  certificateDesc:
    'Create both internal and external certificates, External certificates do require approval from an admin before activating, this may take some time. Try to limit your use of external certificates unless completely necessary.',
  noTransferOwnerAvailable:
    'Certificate may not be approved or rejected.Please follow the instructions mentioned in email',
  certificateGuide1:
    'Ensure you are a member of the self-service administration group in AD for your respective application. You can request access from the owner of your application at <a href="https://access.t-mobile.com" target="_blank">Cloud Access</a>. If you are not a member of one or more of these groups you will not be able to create certificates yourself.',
  certificateGuide2:
    'Currently only the standard SSL Certificate template is used to create certificates, this includes: <br/> Signature algorithm: SHA256-RSA. <br/>Key usage : digitalSignature, keyEncipherment. <br/>Extended key usage : serverAuth for Internal and ServerAuth, ClientAuth for External certificates',
  certificateGuide3:
    'If you need a non-standard certificate please reach out to Cloud Support work with them to create this for you. This functionality will be developed and incorporated into T-Vault at a later date.',
  certificateGuide4:
    'External certificate requests require approval from the NCLM team, that process is outlined when you request an external certificate.',
  certificateGuide5:
    'Suggestions for improvements and features are welcome, please reach out to T-Vault@T-Mobile.com if you have any to share. For more information on how to manage certificates please go <a href="https://ccoe.docs.t-mobile.com/t-vault/user-interface/manage_certificates/" target="_blank">here</a>.',
  iamServiceAccountDesc:
    'IAM Service Accounts can only be modified in AWS/IAM. You can only view the details of this IAM Service Account and rotate the associated secret within T-vault. The associated secret needs to be rotated to active the account.',
  noAppRolesAvailable:
    'Once you create a <strong>New Approle</strong> you’ll be able to add <strong>Secret</strong> to view them all here!',
  azurePrincipal:
    'T-Vault can be used to manage secrets of service principals in Azure Active Directory. In order to self-service your service principals in Azure Active Directory there is a three-step process to onboard the account into T-Vault.',
  azureGuide1:
    '<strong>On-Boarding:</strong>This step brings the AAD service principals into T-Vault so that the secrets can be read or rotated through T-Vault. This is a one-time operation.',
  azureGuide2:
    '<strong>Service Principal Activation:</strong>The AAD service principal owner will Activate (rotate) the account password once after on-boarding the account into T-Vault. This process ensures that the secrets in T-Vault and Azure Active Directory are in sync.',
  azureGuide3:
    '<strong>Granting Permissions:</strong>When an AAD service principal is activated in T-Vault, the service principal owner can grant specific permissions to other users and groups allowing others to read and/or rotate the secrets for the AAD service principal as well through T-Vault.',
  azureActivateConfirmation:
    'During the activation, the password of the Azure service principal will be rotated to ensure Azure and T-Vault are in sync. If you want to continue with activation now please click the "ACTIVATE" button below and make sure to update any services depending on the service principal with its new password.',
};

export default { Resources };
