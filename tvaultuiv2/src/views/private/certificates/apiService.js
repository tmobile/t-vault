import api from '../../../services';

const getNonAdminAppNameList = () => api.get('/sslcert/grouplist');
const getAllAdminCertInternal = (limit, offset) =>
  api.get(`/sslcert/certificates/internal?limit=${limit}&offset=${offset}`);
const getAllAdminCertExternal = (limit, offset) =>
  api.get(`/sslcert?certType=external&limit=${limit}&offset=${offset}`);
const getAllNonAdminCertInternal = () => api.get(`/sslcert/list/internal`);
const getAllNonAdminCertExternal = () => api.get('/sslcert/list/external');
const getInternalCertificates = (limit, offset) =>
  api.get(
    `/sslcert?certificateName=&certType=internal&limit=${limit}&offset=${offset}`
  );
const searchAllCert = () => api.get(`/sslcert/allcertificates`);
const getExternalCertificates = (limit, offset) =>
  api.get(
    `/sslcert?certificateName=&certType=external&limit=${limit}&offset=${offset}`
  );
const deleteCertificate = (name, certType) =>
  api.delete(`/certificates/${name}/${certType}`);
const getOnboardCertificates = () => api.get(`/sslcert/pendingcertificates`);

const getCertificateDetail = (url) => api.get(url);

const addCertificateUser = (payload) => api.post('/sslcert/user', payload);
const deleteCertificateUser = (payload) => api.delete('/sslcert/user', payload);

const addCertificateGroup = (payload) => api.post('/sslcert/group', payload);
const deleteCertificateGroup = (payload) =>
  api.delete('/sslcert/group', payload);

// Api Calls for aws application
const addAwsPermission = (url, payload) => api.post(url, payload);
const addAwsRole = (payload) => api.post('/sslcert/aws', payload);
const deleteAwsRole = (payload) => api.delete('/sslcert/aws', payload);

// Api call for app roles permission
const addAppRolePermission = (payload) => api.post('/sslcert/approle', payload);
const deleteAppRolePermission = (payload) =>
  api.delete('/sslcert/approle', payload);

const getApplicationName = () => api.get('/serviceaccounts/cwm/approles');
const getOwnerEmail = (corpId) => api.get(`/ldap/corpusers?CorpId=${corpId}`);
const createCertificate = (payload) => api.post('/sslcert', payload);
const checkCertificateStatus = (url) => api.get(url);
const certificateRenew = (certType, name) =>
  api.post(`/certificates/${certType}/${name}/renew`);
const getRevokeReason = (id) =>
  api.get(`/certificates/${id}/revocationreasons`);
const revokeRequest = (certType, name, payload) =>
  api.post(`/certificates/${certType}/${name}/revocationrequest`, payload);
const validateExternalCert = (name, certType) =>
  api.get(`/sslcert/validate/${name}/${certType}`);

const getUserName = (user) => api.get(`/ldap/ntusers?displayName=${user}`);
const getTmoUsers = (user) => api.get(`/tmo/users?UserPrincipalName=${user}`);
const transferOwner = (certType, name, ownerName) =>
  api.put(`/sslcert/${certType}/${name}/${ownerName}/transferowner`);

const onDownloadCertificate = (name, format, certType) =>
  api.get(`/sslcert/certificates/${name}/${format}/${certType}`);
const onPrivateDownload = (payload) =>
  api.post('/sslcert/certificates/download', payload);

const onReleasecertificate = (name, type, reason) =>
  api.post(`/sslcert/unlink/${name}/${type}/${reason}`);

const getNotificationEmails = (appId) =>
  api.get(`/serviceaccounts/cwm/appdetails/appname?appName=${appId}`);

const updateCert = (payload) => api.put('/sslcert/', payload);
const onOnboardcertificate = (payload) =>
  api.post('/sslcert/onboardSSLcertificate', payload);

const searchByGroupEmail = (name) => api.get(`/azure/email?mail=${name}`);

export default {
  getAllAdminCertInternal,
  getAllAdminCertExternal,
  getAllNonAdminCertInternal,
  getAllNonAdminCertExternal,
  getInternalCertificates,
  getExternalCertificates,
  deleteCertificate,
  getCertificateDetail,
  addCertificateUser,
  deleteCertificateUser,
  addCertificateGroup,
  deleteCertificateGroup,
  getApplicationName,
  getUserName,
  getTmoUsers,
  getOwnerEmail,
  createCertificate,
  checkCertificateStatus,
  certificateRenew,
  getRevokeReason,
  revokeRequest,
  transferOwner,
  onDownloadCertificate,
  onPrivateDownload,
  onReleasecertificate,
  addAwsPermission,
  addAwsRole,
  deleteAwsRole,
  addAppRolePermission,
  deleteAppRolePermission,
  getNotificationEmails,
  getNonAdminAppNameList,
  getOnboardCertificates,
  updateCert,
  onOnboardcertificate,
  searchByGroupEmail,
  validateExternalCert,
  searchAllCert,
};
