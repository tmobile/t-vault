import React, { useState, useEffect } from 'react';
import { css } from 'styled-components';
import PropTypes from 'prop-types';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import apiService from '../../apiService';

const customStyle = css`
  height: 100%;
`;
const ServiceAccountSecrets = (props) => {
  const { accountDetail } = props;
  const [response, setResponse] = useState({ status: 'loading' });

  useEffect(() => {
    if (accountDetail && Object.keys(accountDetail).length > 0) {
      apiService
        .getServiceAccountPassword(accountDetail?.name)
        .then((res) => {
          setResponse({ status: '' });
          console.log('res', res);
        })
        .catch((e) => console.log('e.response', e.response));
    }
  }, [accountDetail]);

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        secrets
      </>
    </ComponentError>
  );
};

ServiceAccountSecrets.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default ServiceAccountSecrets;
