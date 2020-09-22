import axios from 'axios';
import config from '../config';

function ApiCall(url, method, data, header) {
  const token = 's.GignbOM5ssdYR6D7Cx1sD3xo';
  const headers = {
    ...header,
    'vault-token': token,
    'content-type': 'application/json',
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
