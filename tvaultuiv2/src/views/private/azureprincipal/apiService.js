import api from '../../../services';

const getManageAzureService = () => api.get('/azureserviceaccounts');
const getAzureServiceList = () => api.get('/azureserviceaccounts/list');

const getAzureserviceDetails = (name) =>
  api.get(`/azureserviceaccounts/${name}`);
const activateAzureAccount = (name) =>
  api.post(
    `/azureserviceaccounts/activateAzureServicePrincipal?servicePrincipalName=${name}`
  );

const getAzureSecrets = (name) =>
  api.get(`/azureserviceaccounts/folders/secrets?path=azuresvcacc/${name}`);

const getSecretFolderData = (value) =>
  api.get(`/azureserviceaccounts/secrets/${value}`);

const rotateSecret = (payload) =>
  api.post('/azureserviceaccounts/rotate', payload);

const addUserPermission = (payload) =>
  api.post('/azureserviceaccounts/user', payload);
const deleteUserPermission = (payload) =>
  api.delete('/azureserviceaccounts/user', payload);

const addGroupPermission = (payload) =>
  api.post('/azureserviceaccounts/group', payload);
const deleteGroupPermission = (payload) =>
  api.delete('/azureserviceaccounts/group', payload);

const addAwsPermission = (url, payload) => api.post(url, payload);
const addAwsRole = (payload) => api.post('/azureserviceaccounts/role', payload);
const deleteAwsRole = (payload) =>
  api.delete('/azureserviceaccounts/role', payload);

const addAppRolePermission = (payload) =>
  api.post('/azureserviceaccounts/approle', payload);
const deleteAppRolePermission = (payload) =>
  api.delete('/azureserviceaccounts/approle', payload);

export default {
  getManageAzureService,
  getAzureServiceList,
  getAzureserviceDetails,
  activateAzureAccount,
  getAzureSecrets,
  getSecretFolderData,
  rotateSecret,
  addUserPermission,
  deleteUserPermission,
  addGroupPermission,
  deleteGroupPermission,
  addAwsPermission,
  addAwsRole,
  deleteAwsRole,
  addAppRolePermission,
  deleteAppRolePermission,
};
