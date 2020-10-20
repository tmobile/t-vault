import api from '../../../services';

// API call to get all service accoounts
const getServiceAccountList = () => api.get('/serviceaccounts/list');
const getServiceAccounts = () => api.get('/serviceaccounts');

// Offboard service account
const offBoardServiceAccount = (payload) =>
  api.post('/serviceaccounts/offboard', payload);

// Transfer owner service account
const transferOwner = (svcName) =>
  api.post(`/serviceaccounts/transfer?serviceAccountName=${svcName}`);

const getAppRoles = () => api.get('/serviceaccounts/cwm/approles');
const getUsersServiceAccounts = (name) =>
  api.get(`/ad/serviceaccounts?serviceAccountName=${name}`);
const onBoardServiceAccount = (payload) =>
  api.post('/serviceaccounts/onboard', payload);
const updateServiceAccount = (payload) =>
  api.put('/serviceaccounts/onboard', payload);
const fetchServiceAccountDetails = (svcName) =>
  api.get(
    `/ad/serviceaccounts?serviceAccountName=${svcName}&excludeOnboarded=false`
  );
const callServiceAccount = (svcName) => api.get(`/serviceaccounts/${svcName}`);
const updateMetaPath = (svcName) =>
  api.get(`/serviceaccounts/meta?path=ad/roles/${svcName}`);

const activateServiceAccount = (svcName) =>
  api.put(`/serviceaccounts/password?serviceAccountName=${svcName}`);
// Service account secret API call.
const getServiceAccountPassword = (svcName) =>
  api.get(`/serviceaccounts/password?serviceAccountName=${svcName}`);
const resetServiceAccountPassword = (svcName, payload) =>
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

export default {
  getServiceAccounts,
  getServiceAccountList,
  transferOwner,
  getServiceAccountPassword,
  resetServiceAccountPassword,
  getAppRoles,
  onBoardServiceAccount,
  fetchServiceAccountDetails,
  callServiceAccount,
  updateMetaPath,
  getUsersServiceAccounts,
  offBoardServiceAccount,
  addUserPermission,
  deleteUserPermission,
  activateServiceAccount,
  updateServiceAccount,
  addGroupPermission,
  deleteGroupPermission,
  addAwsPermission,
  addAwsRole,
  addAppRolePermission,
  deleteAppRolePermission,
  deleteAwsRole,
};
