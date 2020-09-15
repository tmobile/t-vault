import api from '../../../services';
import { mockApi } from '../../../services/helper-function';

const fetchSafe = (data) => mockApi(data);
const searchUser = (data) => mockApi(data);

const getApiCall = (path, payload, header) => api.get(path, payload, header);
const postApiCall = (path, payload, header) => api.post(path, payload, header);
const deleteApiCall = (path, payload, header) =>
  api.delete(path, payload, header);
const putApiCall = (path, payload, header) => api.put(path, payload, header);

export default {
  fetchSafe,
  searchUser,
  postApiCall,
  getApiCall,
  deleteApiCall,
  putApiCall,
};
