import api from '../../../services';

const getManageAzureService = () => api.get('/azureserviceaccounts');
const getAzureServiceList = () => api.get('/azureserviceaccounts/list');

const getAzureserviceDetails = (name) =>
  api.get(`/azureserviceaccounts/${name}`);
const activateAzureAccount = (name) =>
  api.post(
    `/azureserviceaccounts/activateAzureServicePrincipal?servicePrincipalNameg=${name}`
  );

export default {
  getManageAzureService,
  getAzureServiceList,
  getAzureserviceDetails,
  activateAzureAccount,
};
