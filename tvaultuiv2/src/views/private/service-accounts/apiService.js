import api from '../../../services';

const getServiceAccountList = () => api.get('/serviceaccounts/list');
const getServiceAccounts = () => api.get('/serviceaccounts');

const getServiceAccountPassword = (name) =>
  api.get(`/serviceaccounts/password?serviceAccountName=${name}`);

export default {
  getServiceAccounts,
  getServiceAccountList,
  getServiceAccountPassword,
};
