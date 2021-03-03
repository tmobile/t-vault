import api from '../../../services';

const getSafes = (offset) => api.get(`/ss/sdb/safes?limit=20&offset=${offset}`);
const deleteSafe = (path) => api.delete(`/ss/sdb/delete?path=${path}`);
const getManageUsersList = (offset) =>
  api.get(`/ss/sdb/list?path=users&limit=20&offset=${offset}`);
const getManageSharedList = (offset) =>
  api.get(`/ss/sdb/list?path=shared&limit=20&offset=${offset}`);
const getManageAppsList = () => api.get('/ss/sdb/list?path=apps');

const getSafeDetails = (path) => api.get(`/ss/sdb?path=${path}`);
const editSafe = (payload) => api.put('/ss/sdb', payload);
const createSafe = (payload) => api.post('/ss/sdb', payload);
const getOwnerEmail = (owner) =>
  api.get(`/ldap/users?UserPrincipalName=${owner}`);
const getTmoUsers = (user) => api.get(`/tmo/users?UserPrincipalName=${user}`);
const addUserPermission = (payload) => api.post('/ss/sdb/user', payload);
const getUsersDetails = (name) => api.get(`/ldap/getusersdetail/${name}`);
const deleteUserPermission = (payload) => api.delete('/ss/sdb/user', payload);
const getUserName = (user) => api.get(`/ldap/ntusers?displayName=${user}`);

const getGroupsName = (group) => api.get(`/azure/groups?name=${group}`);
const deleteGroup = (payload) => api.delete('/ss/sdb/group', payload);
const addGroup = (payload) => api.post('/ss/sdb/group', payload);

const addAwsConfiguration = (url, payload) => api.post(url, payload);
const addAwsRole = (payload) => api.post('/ss/sdb/role', payload);
const deleteAwsConfiguration = (payload) => api.delete('/ss/sdb/role', payload);
const editAwsApplication = (payload) => api.put('/ss/sdb/role', payload);

const getExistingAppRole = () => api.get('/ss/approle');
const addAppRole = (payload) => api.post('/ss/sdb/approle', payload);
const deleteAppRole = (payload) => api.delete('/ss/sdb/approle', payload);

const addFolder = (params) => api.post(`/sdb/createfolder?path=${params}`);
const deleteFolder = (params) => api.delete(`/sdb/delete?path=${params}`);
const modifySecret = (params, payload) =>
  api.post(`/write?path=${params}`, payload);
const getSecret = (params) =>
  api.get(`/safes/folders/secrets?path=${params}&fetchOption=all`);

const getOwnerTransferEmail = (value) =>
  api.get(`/tmo/users?UserPrincipalName=${value}`);
const transferSafeOwner = (payload) => api.post('/ss/transfersafe', payload);

const getApplicationName = () => api.get('/serviceaccounts/cwm/approles');
const getVersionInfo = (path) =>
  api.get(`/safes/folders/versioninfo?path=${path}`);

export default {
  getSafes,
  deleteSafe,
  getManageUsersList,
  getManageAppsList,
  getManageSharedList,
  createSafe,
  getOwnerEmail,
  getTmoUsers,
  deleteUserPermission,
  addUserPermission,
  getUserName,
  addFolder,
  modifySecret,
  getSecret,
  getSafeDetails,
  editSafe,
  deleteFolder,
  getGroupsName,
  addGroup,
  deleteGroup,
  addAwsConfiguration,
  deleteAwsConfiguration,
  addAwsRole,
  deleteAppRole,
  getExistingAppRole,
  addAppRole,
  editAwsApplication,
  getUsersDetails,
  getOwnerTransferEmail,
  transferSafeOwner,
  getApplicationName,
  getVersionInfo,
};
