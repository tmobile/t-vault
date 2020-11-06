/* eslint-disable no-console */
/* eslint-disable no-unreachable */
/* eslint-disable consistent-return */
/* eslint-disable no-alert */
const loginReducer = (state, action) => {
  switch (action.type) {
    case 'CALLBACK_DATA':
      return { ...state, callbackData: action.payload };
      break;
    default:
      break;
  }
};

export default loginReducer;
