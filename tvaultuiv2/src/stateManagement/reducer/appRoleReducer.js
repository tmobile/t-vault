/* eslint-disable no-console */
/* eslint-disable no-unreachable */
/* eslint-disable consistent-return */
/* eslint-disable no-alert */
const appRoleReducer = (state, action) => {
  switch (action.type) {
    case 'UPDATE_APP_ROLE_LIST':
      return { ...state, appRoleList: action.payload };
      break;

    default:
      break;
  }
};

export default appRoleReducer;
