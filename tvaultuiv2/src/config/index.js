const hostName = () => {
  return window.location.origin;
};

let baseUrl = '';

if (process.env.NODE_ENV === 'development') {
  // baseUrl =
  //   process.env.REACT_APP_DEV_HOST_URL ||
  //   'https://stg.perf-vault.corporate.t-mobile.com';
  baseUrl = 'https://perf-vault.corporate.t-mobile.com';
} else {
  baseUrl = hostName();
}

const config = {
  url: `${baseUrl}/vault/v2`,
};

export default config;
