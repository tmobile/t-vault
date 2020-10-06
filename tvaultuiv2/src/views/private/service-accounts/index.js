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

const OnBoardForm = lazy(() => import('./OnBoardForm'));
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
            path="/service-accounts/change-service-accounts"
            render={(routeProps) => (
              <OnBoardForm routeProps={{ ...routeProps }} />
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
