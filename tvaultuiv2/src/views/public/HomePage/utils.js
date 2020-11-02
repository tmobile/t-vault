import apiService from './apiService';

export const initUserLogin = async () => {
  let response;
  try {
    response = await apiService.initiateAuth();
    // eslint-disable-next-line no-console
    console.log(response);
    // eslint-disable-next-line no-debugger
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error('-- Error while initiating authentication process', e);
    response = new Error(e);
    // debugger;
  }
  return response?.data;
};

export default {
  initUserLogin,
};
