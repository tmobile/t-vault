/* eslint-disable react/jsx-props-no-spreading */
import React, { useEffect } from 'react';
import { withRouter } from 'react-router-dom';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import CertificatesDashboard from './components/CertificatesDashboard';
import apiService from './apiService';
import { SectionPreview } from '../../../styles/GlobalStyles';
import { useStateValue } from '../../../contexts/globalState';

const Certificates = (props) => {
  const [, dispatch] = useStateValue();

  useEffect(() => {
    apiService
      .getApplicationName()
      .then((res) => {
        if (res) {
          if (res.data && res.data.length > 0) {
            dispatch({ type: 'APPLICATIONNAME_LIST', payload: [...res.data] });
          }
        }
      })
      .catch(() => {
        dispatch({ type: 'APPLICATIONNAME_LIST', payload: 'error' });
      });
  }, [dispatch]);

  useEffect(() => {
    const username = sessionStorage.getItem('username');
    apiService
      .getOwnerDetails(username)
      .then((res) => {
        if (res.data.data.values && res.data.data.values[0]) {
          if (res.data.data.values[0].userEmail) {
            dispatch({
              type: 'OWNER_EMAIL',
              payload: res.data.data.values[0].userEmail.toLowerCase(),
            });
          }
        }
      })
      .catch(() => {
        dispatch({ type: 'OWNER_EMAIL', payload: 'error' });
      });
  }, [dispatch]);

  return (
    <ComponentError>
      <SectionPreview>
        <CertificatesDashboard {...props} />
      </SectionPreview>
    </ComponentError>
  );
};
Certificates.propTypes = {};

Certificates.defaultProps = {};
export default withRouter(Certificates);
