/* eslint-disable react/jsx-props-no-spreading */
import React, { lazy } from 'react';
import { Route, Switch, withRouter } from 'react-router-dom';
// import PropTypes from 'prop-types';
// eslint-disable-next-line import/no-unresolved
import styled from 'styled-components';
// eslint-disable-next-line import/no-unresolved

import mediaBreakpoints from '../../../breakpoints';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import ServiceAccountDashboard from './components/ServiceAccountDashboard';

const OnBoardServiceAccounts = lazy(() =>
  import('./components/OnBoardServiceAccounts')
);
const ServiceAccountSectionPreview = styled('section')`
  margin: 3em auto;
  height: 77vh;
  ${mediaBreakpoints.small} {
    margin: 0;
    height: 89vh;
  }
`;

const ServiceAccountLayout = (props) => {
  return (
    <ComponentError>
      <main title="service-account-layout">
        <ServiceAccountSectionPreview>
          <ServiceAccountDashboard {...props} />
        </ServiceAccountSectionPreview>
        <Switch>
          <Route
            exact
            // path="/service-accounts/onboard-service-account"
            render={(routeProps) => (
              <OnBoardServiceAccounts routeProps={{ ...routeProps }} />
            )}
          />
          <Route
            exact
            // path="/service-accounts/onboard-service-accounts"
            render={(routeProps) => (
              <OnBoardServiceAccounts routeProps={{ ...routeProps }} />
            )}
          />
        </Switch>
      </main>
    </ComponentError>
  );
};
ServiceAccountLayout.propTypes = {};

ServiceAccountLayout.defaultProps = {};
export default withRouter(ServiceAccountLayout);
