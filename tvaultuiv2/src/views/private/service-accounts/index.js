/* eslint-disable react/jsx-props-no-spreading */
import React, { lazy, useState, useEffect } from 'react';
import { Route, Switch, withRouter } from 'react-router-dom';
// import PropTypes from 'prop-types';
// eslint-disable-next-line import/no-unresolved
import styled from 'styled-components';
// eslint-disable-next-line import/no-unresolved

import mediaBreakpoints from '../../../breakpoints';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import ServiceAccountDashboard from './components/ServiceAccountDashboard';
import SnackbarComponent from '../../../components/Snackbar';
import serviceAccounts from './__mock';

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
  const [moreData, setMoreData] = useState(false);
  const [serviceAccountList, setServiceAccountList] = useState([]);
  const [toast, setToast] = useState(null);
  const [status, setStatus] = useState({});
  // const [activeFolders, setActiveFolders] = useState([]);
  // const [isLoading, setIsLoading] = useState(false);

  // const loadMoreData = () => {
  //   setIsLoading(true);
  // };
  const onToastClose = () => {
    setStatus({});
  };

  useEffect(() => {
    console.log('serviceAccounts', serviceAccounts);
    setServiceAccountList([...serviceAccounts.serviceAccounts]);
  }, []);

  return (
    <ComponentError>
      <main title="service-account-layout">
        <ServiceAccountSectionPreview>
          <ServiceAccountDashboard
            leftColumLists={serviceAccountList}
            status={status}
            moreData={moreData}
            {...props}
          />
        </ServiceAccountSectionPreview>
        <Switch>
          {/* <Route
            exact
            // path="/service-accounts/onboard-service-accounts"
            render={(routeProps) => (
              <OnBoardServiceAccounts routeProps={{ ...routeProps }} />
            )}
          /> */}
          {/* <Route
            exact
            // path="/service-accounts/onboard-service-accounts"
            render={(routeProps) => (
              <OnBoardServiceAccounts routeProps={{ ...routeProps }} />
            )}
          /> */}
        </Switch>
        {toast === -1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message="Something went wrong!"
          />
        )}
        {toast === 1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            message="Service account off-boarded successfully!"
          />
        )}
      </main>
    </ComponentError>
  );
};
ServiceAccountLayout.propTypes = {};

ServiceAccountLayout.defaultProps = {};
export default withRouter(ServiceAccountLayout);
