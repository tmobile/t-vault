import api from '../../../services';

const getServiceAccountList = () => api.get('/serviceaccounts/list');
const getServiceAccounts = () => api.get('/serviceaccounts');

const getServiceAccountPassword = (name) =>
  api.get(`/serviceaccounts/password?serviceAccountName=${name}`);

const resetServiceAccountPassword = (name, payload) =>
  api.put(`/serviceaccounts/password?serviceAccountName=${name}`, payload);

export default {
  getServiceAccounts,
  getServiceAccountList,
  getServiceAccountPassword,
  resetServiceAccountPassword,
};
