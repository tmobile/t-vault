/* eslint-disable no-console */
/* eslint-disable no-unreachable */
/* eslint-disable consistent-return */
/* eslint-disable no-alert */
const loginReducer = (state, action) => {
  switch (action.type) {
    case 'CALLBACK_DATA':
      return { ...state, value: action.data };
      break;

    case 'R1_DUMMY_TWO':
      return { ...state, value: 'Data One - Dummy Two' };
      break;

    default:
      break;
  }
};

export default loginReducer;
