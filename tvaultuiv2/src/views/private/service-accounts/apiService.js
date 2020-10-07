import api from '../../../services';

const getServiceAccountList = () => api.get('/serviceaccounts/list');
const getServiceAccounts = () => api.get('/serviceaccounts');

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

const getServiceAccountPassword = (name) =>
  api.get(`/serviceaccounts/password?serviceAccountName=${name}`);

const resetServiceAccountPassword = (name, payload) =>
  api.put(`/serviceaccounts/password?serviceAccountName=${name}`, payload);

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
};
