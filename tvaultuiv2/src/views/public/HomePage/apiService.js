// https://perf-vault.corporate.t-mobile.com/vault/v2/auth/tvault/renew
// https://perf-vault.corporate.t-mobile.com/app/Messages/uimessages.properties
// https://perf-vault.corporate.t-mobile.com/vault/v2/auth/oidc/auth_url

import api from '../../../services';

const getAuth = () => api.get('/auth/tvault/renew');
const getUserName = () => api.get('/username');
export default {
  getAuth,
  getUserName,
};
