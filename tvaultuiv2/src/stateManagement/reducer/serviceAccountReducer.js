/* eslint-disable no-console */
/* eslint-disable no-unreachable */
/* eslint-disable consistent-return */
/* eslint-disable no-alert */
const serviceAccountReducer = (state, action) => {
  switch (action.type) {
    case 'GET_ALL_SERVICE_ACCOUNT_LIST':
      return { ...state, serviceAccountList: action.payload };
      break;

    default:
      break;
  }
};

export default serviceAccountReducer;
