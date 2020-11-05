const initilaState = {
  message: 'Welcome',
  username: sessionStorage.getItem('username'),
  isAdmin: sessionStorage.getItem('isAdmin'),
  userEmail: sessionStorage.getItem('owner'),
  appRoleList: [],
};

export default initilaState;
