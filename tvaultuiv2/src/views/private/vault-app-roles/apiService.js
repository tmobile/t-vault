import api from '../../../services';
// get calls
const getAppRole = () => api.get('/ss/approle');
const fetchAppRoleDetails = (appRole) => api.get(`/ss/approle/role/${appRole}`);
const getAccessors = (appRole) => api.get(`/ss/approle/${appRole}/accessors`);
const getRoleId = (appRole) => api.get(`/ss/approle/${appRole}/role_id`);

// put calls
const updateAppRole = (payload) => api.put('/ss/approle', payload);

// post calls
const createAppRole = (payload) => api.post('/ss/auth/approle/role', payload);
const createSecretId = (appRole) => api.get(`/ss/approle/${appRole}/secret_id`);

// delete calls
const deleteAppRole = (appRole) =>
  api.delete(`/ss/auth/approle/role/${appRole}`);
const deleteSecretIds = (payload) =>
  api.delete(`/ss/approle/${payload.role_name}/secret_id`, payload);

export default {
  getAppRole,
  fetchAppRoleDetails,
  updateAppRole,
  createAppRole,
  createSecretId,
  getAccessors,
  deleteSecretIds,
  getRoleId,
  deleteAppRole,
};
