/* eslint-disable import/prefer-default-export */
export const userpassResponse = {
  success: false,
  response: {
    client_token: '',
    admin: 'yes',
    access: {},
    policies: ['default', 'safeadmin'],
    lease_duration: 1800000,
  },
  adminPolicies: null,
  httpstatus: 200,
};

export const ldapResponse = {
  client_token: '',
  admin: 'yes',
  access: {},
  policies: ['default', 'safeadmin'],
  lease_duration: 1800,
};
