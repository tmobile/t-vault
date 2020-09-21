import api from '../../../services';
import { mockApi } from '../../../services/helper-function';

const fetchSafe = (data) => mockApi(data);
const searchUser = (data) => mockApi(data);

const getSafes = () => api.get('/vault/v2/ss/sdb/safes');
const getManageUsersList = () => api.get('/vault/v2/ss/sdb/list?path=users');
const getManageSharedList = () => api.get('/vault/v2/ss/sdb/list?path=shared');
const getManageAppsList = () => api.get('/vault/v2/ss/sdb/list?path=apps');

const createSafe = (payload) => api.post('/vault/v2/ss/sdb', payload);
const getOwnerEmail = (owner) =>
  api.get(`/vault/v2/ldap/users?UserPrincipalName=${owner}`);

const addUserPermission = (payload) =>
  api.post('/vault/v2/ss/sdb/user', payload);
const deleteUserPermission = (payload) =>
  api.delete('/vault/v2/ss/sdb/user', payload);

const getUserName = (user) =>
  api.get(`/vault/v2/ldap/corpusers?CorpId=${user}`);

export default {
  fetchSafe,
  searchUser,
  getSafes,
  getManageUsersList,
  getManageAppsList,
  getManageSharedList,
  createSafe,
  getOwnerEmail,
  deleteUserPermission,
  addUserPermission,
  getUserName,
};
