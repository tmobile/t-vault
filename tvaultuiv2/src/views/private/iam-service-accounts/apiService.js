import api from '../../../services';

// API call to get all service accoounts
const getIamServiceAccountList = () => api.get('/iamserviceaccounts/list');
const getIamServiceAccounts = () => api.get('/iamserviceaccounts');

const fetchIamServiceAccountDetails = (svcName) =>
  api.get(`/iamserviceaccounts/${svcName}`);

const activateIamServiceAccount = (svcName, iamAccountId) =>
  api.put(
    `/iamserviceaccount/activate?serviceAccountName=${svcName}&awsAccountId=${iamAccountId}`
  );
// Service account secret API call.
const resetIamServiceAccountPassword = (svcName, payload) =>
  api.put(`/serviceaccounts/password?serviceAccountName=${svcName}`, payload);

// API call for users permission
const addUserPermission = (payload) =>
  api.post('/serviceaccounts/user', payload);

const deleteUserPermission = (payload) =>
  api.delete('/serviceaccounts/user', payload);

// Api call for groups permission
const addGroupPermission = (payload) =>
  api.post('/serviceaccounts/group', payload);
const deleteGroupPermission = (payload) =>
  api.delete('/serviceaccounts/group', payload);

// Api call for aws application permission
const addAwsPermission = (url, payload) => api.post(url, payload);
const addAwsRole = (payload) => api.post('/serviceaccounts/role', payload);
const deleteAwsRole = (payload) => api.delete('/serviceaccounts/role', payload);

// Api call for app roles permission
const addAppRolePermission = (payload) =>
  api.post('/serviceaccounts/approle', payload);
const deleteAppRolePermission = (payload) =>
  api.delete('/serviceaccounts/approle', payload);

//get user details
const getUserDetails = (user) =>
  api.get(`/ldap/getusersdetail/${user}
`);

export default {
  getIamServiceAccounts,
  getIamServiceAccountList,
  resetIamServiceAccountPassword,
  fetchIamServiceAccountDetails,
  addUserPermission,
  deleteUserPermission,
  activateIamServiceAccount,
  addGroupPermission,
  deleteGroupPermission,
  addAwsPermission,
  addAwsRole,
  addAppRolePermission,
  deleteAppRolePermission,
  deleteAwsRole,
  getUserDetails,
};
