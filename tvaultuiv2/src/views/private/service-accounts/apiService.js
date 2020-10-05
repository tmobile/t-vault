import api from '../../../services';

const getServiceAccountList = () => api.get('/serviceaccounts/list');
const getServiceAccounts = () => api.get('/serviceaccounts');

export default { getServiceAccounts, getServiceAccountList };
