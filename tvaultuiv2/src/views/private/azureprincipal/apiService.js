import api from '../../../services';

const getManageAzureService = () => api.get('/azureserviceaccounts');
const getAzureServiceList = () => api.get('/azureserviceaccounts/list');

export default {
  getManageAzureService,
  getAzureServiceList,
};
