const Resources = {
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
};

export default { Resources };
