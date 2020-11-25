const initilaState = {
  message: 'Welcome',
  username: localStorage.getItem('username'),
  isAdmin: localStorage.getItem('isAdmin'),
  userEmail: localStorage.getItem('owner'),
  appRoleList: [],
  iamServiceAccountList: [],
};

export default initilaState;
