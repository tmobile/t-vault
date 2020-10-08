import reducerOne from './reducerOne';
import reducerTwo from './reducerTwo';

const mainReducer = (state, action) => ({
  ...state,
  dataOne: reducerOne(state.dataOne, action),
  dataTwo: reducerTwo(state.dataTwo, action),
});

export default mainReducer;
