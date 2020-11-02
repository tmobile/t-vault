import serviceAccountReducer from './serviceAccountReducer';
import appRoleReducer from './appRoleReducer';
import certificateReducer from './certificateReducer';
import loginReducer from './loginReducer';

const mainReducer = (state, action) => ({
  ...state,
  ...loginReducer(state, action),
  ...serviceAccountReducer(state.serviceAccountList, action),
  ...appRoleReducer(state.appRoleList, action),
  ...certificateReducer(state, action),
});

export default mainReducer;
