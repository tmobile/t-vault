/* eslint-disable no-unreachable */
/* eslint-disable consistent-return */
/* eslint-disable no-alert */
const reducerOne = (state, action) => {
  switch (action.type) {
    case 'R1_DUMMY_ONE':
      alert('In Reducer one - dummy one');
      return { ...state, value: action.data };
      break;

    case 'R1_DUMMY_TWO':
      alert('In Reducer one - dummy two');
      return { ...state, value: 'Data One - Dummy Two' };
      break;

    default:
      break;
  }
};

export default reducerOne;
