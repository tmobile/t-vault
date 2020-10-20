import serviceAccountReducer from './serviceAccountReducer';
import appRoleReducer from './appRoleReducer';

const mainReducer = (state, action) => ({
  ...state,
  ...serviceAccountReducer(state.serviceAccountList, action),
  ...appRoleReducer(state.appRoleList, action),
});

export default mainReducer;
