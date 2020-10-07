import api from '../../../services';

// API call to get all service accoounts
const getServiceAccountList = () => api.get('/serviceaccounts/list');
const getServiceAccounts = () => api.get('/serviceaccounts');

// Onboarding API call
const getAppRoles = () => api.get('/serviceaccounts/cwm/approles');
const getUsersServiceAccounts = (name) =>
  api.get(`/ad/serviceaccounts?serviceAccountName=${name}`);
const onBoardServiceAccount = (payload) =>
  api.post('/serviceaccounts/onboard', payload);
const fetchServiceAccountDetails = (svcName) =>
  api.get(
    `/ad/serviceaccounts?serviceAccountName=${svcName}&excludeOnboarded=false`
  );
const callServiceAccount = (svcName) => api.get(`/serviceaccounts/${svcName}`);
const updateMetaPath = (svcName) =>
  api.get(`/serviceaccounts/meta?path=ad/roles/${svcName}`);

// Service account secret API call.
const getServiceAccountPassword = (name) =>
  api.get(`/serviceaccounts/password?serviceAccountName=${name}`);
const resetServiceAccountPassword = (name, payload) =>
  api.put(`/serviceaccounts/password?serviceAccountName=${name}`, payload);

// Service account permissions API call.
const getServiceAccountDetail = (name) =>
  api.get(`/serviceaccounts/meta?path=ad/roles/${name}`);

export default {
  getServiceAccounts,
  getServiceAccountList,
  getServiceAccountPassword,
  resetServiceAccountPassword,
  getAppRoles,
  onBoardServiceAccount,
  fetchServiceAccountDetails,
  callServiceAccount,
  updateMetaPath,
  getUsersServiceAccounts,
  getServiceAccountDetail,
};
