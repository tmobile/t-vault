// https://perf-vault.corporate.t-mobile.com/vault/v2/auth/tvault/renew

import api from '../services';

const getAuth = () => api.get('/auth/tvault/renew');

export default {
  getAuth,
};
