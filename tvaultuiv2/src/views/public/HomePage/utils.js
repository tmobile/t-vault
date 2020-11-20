/* eslint-disable no-console */
import axios from 'axios';
import configUrl from '../../../config';

export const getUniMessage = () => {
  const url = configUrl.baseUrl.replace('/vault/v2', '');
  axios
    .get(`${url}/app/Messages/uimessages.properties`, {
      headers: { 'vault-token': null, 'Access-Control-Allow-Origin': '*' },
    })
    .then((res) => console.log('res', res))
    .catch((err) => console.log('err', err));
};

export const revokeToken = () => {
  const url = configUrl.baseUrl.replace('/v2', '');
  return axios
    .get(`${url}/auth/tvault/revoke`, {
      headers: { 'vault-token': sessionStorage.getItem('token') },
    })
    .then((res) => console.log('res', res))
    .catch((e) => console.log('e', e));
};

export const renewToken = () => {
  if (sessionStorage.getItem('token')) {
    return axios.get(`${configUrl.baseUrl}/auth/tvault/renew`, {
      headers: { 'vault-token': sessionStorage.getItem('token') },
    });
  }
  return null;
};

export default {
  getUniMessage,
  revokeToken,
  renewToken,
};
