const initilaState = {
  message: 'Welcome',
  username: localStorage.getItem('username'),
  isAdmin: JSON.parse(localStorage.getItem('isAdmin')),
  userEmail: localStorage.getItem('owner'),
  appRoleList: [],
  iamServiceAccountList: [],
};

export default initilaState;
