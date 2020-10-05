import api from '../../../services';

const getServiceAccounts = () => api.get('/serviceaccounts');

export default { getServiceAccounts };
