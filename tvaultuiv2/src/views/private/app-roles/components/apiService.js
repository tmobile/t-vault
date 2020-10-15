import api from '../../../../services';

const getAppRole = () => api.get('/ss/approle');
const fetchAppRoleDetails = (appRole) => api.get(`/ss/approle/role/${appRole}`);
const updateAppRole = (payload) => api.put('/ss/approle', payload);
const createAppRole = (payload) => api.post('/ss/auth/approle/role', payload);
const createSecretId = (appRole) => api.get(`/ss/approle/${appRole}/secret_id`);
const getAccessors = (appRole) => api.get(`/ss/approle/${appRole}/accessors`);

const deleteSecretIds = (payload) =>
  api.delete(`/ss/approle/${payload.role_name}/secret_id`, payload);

const getRoleId = (appRole) => api.get(`/ss/approle/${appRole}/role_id`);
export default {
  getAppRole,
  fetchAppRoleDetails,
  updateAppRole,
  createAppRole,
  createSecretId,
  getAccessors,
  deleteSecretIds,
  getRoleId,
};
