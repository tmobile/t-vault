import config from './config';

const hostName = () => {
  return window.location.origin;
};

let baseUrl = '';
let redirectUrl = '';

if (process.env.NODE_ENV === 'development') {
  baseUrl = config.DEV_ENDPOINT_HOST_NAME;
  redirectUrl = config.DEV_OIDC_REDIRECT_URL;
} else {
  baseUrl = config.OIDC_REDIRECT_URL || hostName();
  redirectUrl = config.OIDC_REDIRECT_URL || hostName();
}

baseUrl = `${baseUrl}/vault/v2`;

export default { baseUrl, redirectUrl };
