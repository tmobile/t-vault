import axios from 'axios';
import config from '../config';

function ApiCall(url, method, data, header) {
  const token = 's.2P19Zf1IkbcnZPG8gcgP1YiU';
  const headers = {
    ...header,
    'vault-token': token,
    'content-type': 'application/json',
    'Access-Control-Allow-Origin': '*',
  };
  return axios.request({ url, method, headers, data });
}

const api = {
  get(path, payload, header) {
    const url = config.url + path;
    return ApiCall(url, 'GET', payload, header);
  },
  post(path, payload, header) {
    const url = config.url + path;
    return ApiCall(url, 'POST', payload, header);
  },
  put(path, payload, header) {
    const url = config.url + path;
    return ApiCall(url, 'PUT', payload, header);
  },
  delete(path, payload, header) {
    const url = config.url + path;
    return ApiCall(url, 'DELETE', payload, header);
  },
};

export default api;
