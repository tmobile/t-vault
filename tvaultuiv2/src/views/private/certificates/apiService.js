import api from '../../../services';

const getAllAdminCertInternal = () => api.get('/sslcert/certificates/internal');
const getAllNonAdminCertInternal = () => api.get('/sslcert/list/internal');
const getAllNonAdminCertExternal = () => api.get('/sslcert/list/external');
const getInternalCertificates = () =>
  api.get('/sslcert?certificateName=&certType=internal');
const getExternalCertificates = () =>
  api.get('/sslcert?certificateName=&certType=external');
const deleteCertificate = (name, certType) =>
  api.delete(`/certificates/${name}/${certType}`);

const getCertificateDetail = (url) => api.get(url);

const addCertificateUser = (payload) => api.post('/sslcert/user', payload);
const deleteCertificateUser = (payload) => api.delete('/sslcert/user', payload);

const addCertificateGroup = (payload) => api.post('/sslcert/group', payload);
const deleteCertificateGroup = (payload) =>
  api.delete('/sslcert/group', payload);

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

const getOwnerTransferEmail = (owner) =>
  api.get(`/ldap/users?UserPrincipalName=${owner}`);
const transferOwner = (certType, name, ownerName) =>
  api.put(`/sslcert/${certType}/${name}/${ownerName}/transferowner`);

const onDownloadCertificate = (name, format, certType) =>
  api.get(`/sslcert/certificates/${name}/${format}/${certType}`);
const onPrivateDownload = (payload) =>
  api.post('/sslcert/certificates/download', payload);

const onReleasecertificate = (name, type, reason) =>
  api.post(`/sslcert/unlink/${name}/${type}/${reason}`);

export default {
  getAllAdminCertInternal,
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
  getOwnerEmail,
  createCertificate,
  checkCertificateStatus,
  certificateRenew,
  getRevokeReason,
  revokeRequest,
  getOwnerTransferEmail,
  transferOwner,
  onDownloadCertificate,
  onPrivateDownload,
  onReleasecertificate,
};
