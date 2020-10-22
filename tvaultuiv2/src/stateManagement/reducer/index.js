import serviceAccountReducer from './serviceAccountReducer';
import appRoleReducer from './appRoleReducer';
import certificateReducer from './certificateReducer';

const mainReducer = (state, action) => ({
  ...state,
  ...serviceAccountReducer(state.serviceAccountList, action),
  ...appRoleReducer(state.appRoleList, action),
  ...certificateReducer(state, action),
});

export default mainReducer;
