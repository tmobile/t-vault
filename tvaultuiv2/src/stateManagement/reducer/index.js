import serviceAccountReducer from './serviceAccountReducer';

const mainReducer = (state, action) => ({
  ...state,
  ...serviceAccountReducer(state.serviceAccountList, action),
});

export default mainReducer;
