/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
import styled from 'styled-components';

import mediaBreakpoints from '../../../breakpoints';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import CertificatesDashboard from './components/CertificatesDashboard';

const CertificatesSectionPreview = styled('section')`
  margin: 3em auto;
  height: 77vh;
  ${mediaBreakpoints.semiLarge} {
    margin: 3rem 3.5rem 0 3.5rem;
  }
  ${mediaBreakpoints.small} {
    margin: 0;
    height: 89vh;
  }
`;

const Certificates = (props) => {
  return (
    <ComponentError>
      <main title="service-account-layout">
        <CertificatesSectionPreview>
          <CertificatesDashboard {...props} />
        </CertificatesSectionPreview>
      </main>
    </ComponentError>
  );
};
Certificates.propTypes = {};

Certificates.defaultProps = {};
export default withRouter(Certificates);
