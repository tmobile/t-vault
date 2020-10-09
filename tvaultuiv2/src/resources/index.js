const Resources = {
  serviceAccount:
    "Service Account Activation. By default passwords are not set to autorotate <br/> <p>Note: When 'Enable Password Rotation' is turned off, the password for this service account will not be autorotated by T-Vault.</p>",
  offBoardConfirmation:
    'Are you sure you want to offboard this Service Account? This will not delete the Service Account from AD server.',
  offBoardSuccessfull:
    'Offboarding of Service Account has been completed successfully. The Service Account Password can no longer be managed by T-Vault. For security reasons, you need to log out and log in order for the changes to effect.',
  noUsersPermissionFound:
    'No <strong>users</strong> are given permission to access this safe, add users to access the safe',
};

export default { Resources };
