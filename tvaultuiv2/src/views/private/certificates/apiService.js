import api from '../../../services';

const getAllAdminCertInternal = () => api.get('/sslcert/certificates/internal');
const getAllNonAdminCertInternal = () => api.get('/sslcert/list/internal');
const getAllNonAdminCertExternal = () => api.get('/sslcert/list/external');
const getInternalCertificates = () =>
  api.get('/sslcert?certificateName=&certType=internal');
const getExternalCertificates = () =>
  api.get('/sslcert?certificateName=&certType=external');

const getCertificateDetail = (url) => api.get(url);

const addCertificateUser = (payload) => api.post('/sslcert/user', payload);
const deleteCertificate = (payload) => api.delete('/sslcert/user', payload);

const addCertificateGroup = (payload) => api.post('/sslcert/group', payload);
const deleteCertificateGroup = (payload) =>
  api.delete('/sslcert/group', payload);

const getApplicationName = () => api.get('/serviceaccounts/cwm/approles');
const getOwnerEmail = (corpId) => api.get(`/ldap/corpusers?CorpId=${corpId}`);
const createCertificate = (payload) => api.post('/sslcert', payload);
const checkExtCertificateStatus = (name, certType) =>
  api.get(`/sslcert/checkstatus/${name}/${certType}`);
const certificateRenew = (certType, name) =>
  api.post(`/certificates/${certType}/${name}/renew`);
const getRevokeReason = (id) =>
  api.get(`/certificates/${id}/revocationreasons`);
const revokeRequest = (certType, name, payload) =>
  api.post(`/certificates/${certType}/${name}/revocationrequest`, payload);

const getOwnerTransferEmail = (owner) =>
  api.get(`/ldap/users?UserPrincipalName=${owner}`);
const transferOwner = (certType, name, ownerName) =>
  api.put(`/sslcert/${certType}/${name}/${ownerName}/transferowner`);

export default {
  getAllAdminCertInternal,
  getAllNonAdminCertInternal,
  getAllNonAdminCertExternal,
  getInternalCertificates,
  getExternalCertificates,
  getCertificateDetail,
  addCertificateUser,
  deleteCertificate,
  addCertificateGroup,
  deleteCertificateGroup,
  getApplicationName,
  getOwnerEmail,
  createCertificate,
  checkExtCertificateStatus,
  certificateRenew,
  getRevokeReason,
  revokeRequest,
  getOwnerTransferEmail,
  transferOwner,
};
