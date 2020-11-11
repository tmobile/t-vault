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
  noUsersPermissionFound:
    'No <strong>Users</strong> are given permission to access this safe, add users to access the safe',
  noGroupsPermissionFound:
    'No <strong>Groups</strong> are given permission to access this safe, add groups to access the safe',
  noAwsPermissionFound:
    'No <strong>Applications</strong> are given permission to access this safe, add applications to access the safe',
  noAppRolePermissionFound:
    'No <strong>App roles</strong> are given permission to access this safe, add approles to access the safe',
  transferConfirmation:
    'Are you sure you want to transfer service account owner?',
  noCertificatesFound:
    'Once you add a <strong>Certificate</strong> you’ll see the  Corresponding <strong>Details</strong> here!',
  appRoles:
    'AppRoles operate a lot like safes, but they put the aplication as the logical unit for sharing. Additional Accersor ID and Secret ID pairs can easily be created through T-Vault, Secret IDs can only be accessed when downloaded',
  certificateDesc:
    'Create both internal and external certificates, External certificates do require approval from an admin before activating, this may take some time. Try to limit your use of external certificates unless completely necessary.',
  noTransferOwnerAvailable:
    'Certificate may not be approved or rejected.Please follow the instructions mentioned in email',
  certificateGuide1:
    'Ensure you are a member of the self-service administration group in AD for your respective application. You can request access from the owner of your application at <a href="https://access.t-mobile.com/">Cloud Access</a>. If you are not a member of one or more of these groups you will not be able to create certificates yourself.',
  certificateGuide2:
    'Currently only the standard SSL Certificate template is used to create certificates, this includes: <br/> Signature algorithm: SHA256-RSA. <br/>Key usage : digitalSignature, keyEncipherment. <br/>Extended key usage : serverAuth for Internal and ServerAuth, ClientAuth for External certificates',
  certificateGuide3:
    'If you need a non-standard certificate please reach out to Cloud Support work with them to create this for you. This functionality will be developed and incorporated into T-Vault at a later date.',
  certificateGuide4:
    'External certificate requests require approval from the NCLM team, that process is outlined when you request an external certificate.',
  certificateGuide5:
    'Suggestions for improvements and features are welcome, please reach out to T-Vault@T-Mobile.com if you have any to share. For more information on how to manage certificates please go <a href="https://ccoe.docs.t-mobile.com/t-vault/user-interface/manage_certificates/">here</a>.',
};

export default { Resources };
