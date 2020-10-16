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
};
