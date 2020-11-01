const initilaState = {
  message: 'Welcome',
  username: sessionStorage.getItem('username'),
  isAdmin: sessionStorage.getItem('isAdmin'),
  appRoleList: [],
};

export default initilaState;
