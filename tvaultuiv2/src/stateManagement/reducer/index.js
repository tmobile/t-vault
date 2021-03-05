import serviceAccountReducer from './serviceAccountReducer';
import appRoleReducer from './appRoleReducer';
import certificateReducer from './certificateReducer';
import iamServiceAccountReducer from './iamServiceAccountReducer';
import safeReducer from './safeReducer';
import loginReducer from './loginReducer';

const mainReducer = (state, action) => ({
  ...state,
  ...loginReducer(state, action),
  ...serviceAccountReducer(state.serviceAccountList, action),
  ...appRoleReducer(state.appRoleList, action),
  ...certificateReducer(state, action),
  ...iamServiceAccountReducer(state, action),
  ...safeReducer(state, action),
});

export default mainReducer;
