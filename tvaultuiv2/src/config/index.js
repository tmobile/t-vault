let baseUrl = '';
if (process.env.NODE_ENV === 'development') {
  baseUrl =
    process.env.REACT_APP_DEV_HOST_URL ||
    'https://perf-vault.corporate.t-mobile.com/vault/v2';
} else {
  baseUrl =
    process.env.REACT_APP_DEV_HOST_URL ||
    'https://perf-vault.corporate.t-mobile.com/vault/v2';
}
const config = {
  url: baseUrl,
};

export default config;
