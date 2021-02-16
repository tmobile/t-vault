/* eslint-disable no-console */
/* eslint-disable no-unreachable */
/* eslint-disable consistent-return */
/* eslint-disable no-alert */
const iamServiceAccountReducer = (state, action) => {
    switch (action.type) {
      case 'GET_ALL_IAM_SERVICE_ACCOUNT_LIST':
        return { ...state, iamServiceAccountList: action.payload };
        break;
  
      default:
        break;
    }
  };
  
  export default iamServiceAccountReducer;
  