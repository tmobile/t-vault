/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
// eslint-disable-next-line import/no-unresolved
import styled from 'styled-components';
// eslint-disable-next-line import/no-unresolved

import mediaBreakpoints from '../../../breakpoints';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import ServiceAccountDashboard from './components/ServiceAccountDashboard';
// import { UserContext } from '../../../contexts';
// import { useStateValue } from '../../../contexts/globalState';
const ServiceAccountSectionPreview = styled('section')`
  margin: 3em auto;
  height: 77vh;
  ${mediaBreakpoints.small} {
    margin: 0;
    height: 89vh;
  }
`;

const ServiceAccountLayout = (props) => {
  // const userInfo = useContext(UserContext);
  // eslint-disable-next-line no-console

  return (
    <ComponentError>
      <main title="service-account-layout">
        <ServiceAccountSectionPreview>
          <ServiceAccountDashboard {...props} />
        </ServiceAccountSectionPreview>
      </main>
    </ComponentError>
  );
};
ServiceAccountLayout.propTypes = {};

ServiceAccountLayout.defaultProps = {};
export default withRouter(ServiceAccountLayout);
