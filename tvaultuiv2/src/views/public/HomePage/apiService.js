// https://perf-vault.corporate.t-mobile.com/vault/v2/auth/tvault/renew
// https://perf-vault.corporate.t-mobile.com/app/Messages/uimessages.properties
// https://perf-vault.corporate.t-mobile.com/vault/v2/auth/oidc/auth_url

import api from '../../../services';
// import axios from 'axios';

const getAuth = () => api.get('/auth/tvault/renew');
const initiateAuth = () =>
  api.post('/auth/oidc/auth_url', {
    role: 'default',
    redirect_uri: 'http://localhost:3000',
  });

export default {
  getAuth,
  initiateAuth,
};
