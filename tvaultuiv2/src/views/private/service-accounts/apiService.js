import api from '../../../services';

const getAppRoles = () => api.get('/serviceaccounts/cwm/approles');
const getServiceAccounts = (name) =>
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
export default {
  getAppRoles,
  getServiceAccounts,
  onBoardServiceAccount,
  fetchServiceAccountDetails,
  callServiceAccount,
  updateMetaPath,
};
