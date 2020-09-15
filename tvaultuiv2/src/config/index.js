let url = '';
if (process.env.NODE_ENV === 'development') {
  url = process.env.REACT_APP_DEV_HOST_URL;
} else {
  url = process.env.REACT_APP_DEV_HOST_URL;
}
const config = {
  url,
};

export default config;
