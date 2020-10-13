import api from '../../../services';

const getAllCertInternal = () => api.get('/sslcert/certificates/internal');
const getInternalCertificates = () =>
  api.get('/sslcert?certificateName=&certType=internal');
const getExternalCertificates = () =>
  api.get('/sslcert?certificateName=&certType=external');

export default {
  getAllCertInternal,
  getInternalCertificates,
  getExternalCertificates,
};
