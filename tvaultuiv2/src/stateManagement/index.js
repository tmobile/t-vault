const initilaState = {
  message: 'Welcome',
  username: sessionStorage.getItem('username'),
  isAdmin: sessionStorage.getItem('isAdmin'),
  userEmail: sessionStorage.getItem('owner'),
  appRoleList: [],
  iamServiceAccountList:[]
};

export default initilaState;
