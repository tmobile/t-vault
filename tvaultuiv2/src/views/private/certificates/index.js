/* eslint-disable no-console */
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
    if (!JSON.parse(sessionStorage.getItem('isAdmin'))) {
      apiService
        .getNonAdminAppNameList()
        .then((res) => {
          if (res) {
            sessionStorage.setItem('selfServiceAppNames', res.data);
          }
        })
        .catch((err) => console.log('err', err));
    }
  }, []);

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
