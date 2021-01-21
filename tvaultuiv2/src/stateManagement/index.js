const initilaState = {
  message: 'Welcome',
  username: sessionStorage.getItem('username')?.toLowerCase(),
  isAdmin: JSON.parse(sessionStorage.getItem('isAdmin')),
  userEmail: sessionStorage.getItem('owner'),
  appRoleList: [],
  iamServiceAccountList: [],
};

export default initilaState;
