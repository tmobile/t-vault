import api from '../../../services';
import { mockApi } from '../../../services/helper-function';

const fetchSafe = (data) => mockApi(data);
const searchUser = (data) => mockApi(data);

const getSafes = () => api.get('/ss/sdb/safes');
const getManageUsersList = () => api.get('/ss/sdb/list?path=users');
const getManageSharedList = () => api.get('/ss/sdb/list?path=shared');
const getManageAppsList = () => api.get('/ss/sdb/list?path=apps');

const createSafe = (payload) => api.post('/ss/sdb', payload);
const getOwnerEmail = (owner) =>
  api.get(`/ldap/users?UserPrincipalName=${owner}`);

const addUserPermission = (payload) => api.post('/ss/sdb/user', payload);
const deleteUserPermission = (payload) => api.delete('/ss/sdb/user', payload);

const getUserName = (user) => api.get(`/ldap/corpusers?CorpId=${user}`);
const addFolder = (params) => api.post(`/sdb/createfolder?path=${params}`);
const addSecret = (params) => api.post(`/write?path=${params}`);
const getSecret = (params) =>
  api.get(`/safes/folders/secrets?path=${params}&fetchOption=all`);
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
  addFolder,
  addSecret,
  getSecret,
};
