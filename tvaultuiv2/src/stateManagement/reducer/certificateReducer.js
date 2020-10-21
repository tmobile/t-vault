/* eslint-disable consistent-return */
/* eslint-disable no-unreachable */

const certificateReducer = (state, action) => {
  switch (action.type) {
    case 'APPLICATIONNAME_LIST':
      return { ...state, applicationNameList: action.payload };
      break;
    case 'OWNER_EMAIL':
      return { ...state, owner: action.payload };
      break;
    default:
      break;
  }
};

export default certificateReducer;
