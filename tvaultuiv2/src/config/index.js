let baseUrl = '';
if (process.env.NODE_ENV === 'development') {
  baseUrl =
    process.env.REACT_APP_DEV_HOST_URL ||
    'https://dev-vault.corporate.t-mobile.com';
} else {
  baseUrl =
    process.env.REACT_APP_DEV_HOST_URL ||
    'https://dev-vault.corporate.t-mobile.com';
}
const config = {
  url: baseUrl,
};

export default config;
