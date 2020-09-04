import React, { Suspense, lazy } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';

import SafePageLayout from './private/SafePageLayout';

const Home = lazy(() => import('./public/HomePage'));
const Applications = lazy(() => import('./private/applications'));
const Certificates = lazy(() => import('./private/certificates'));
const ServiceAccounts = lazy(() => import('./private/service-accounts'));

const PrivateRoutes = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Switch>
        <Redirect exact from="/" to="/safe" />
        <Route
          path="/applications"
          render={(routeProps) => <Applications routeProps={routeProps} />}
        />
        <Route
          path="/certificates"
          render={(routeProps) => <Certificates routeProps={routeProps} />}
        />
        <Route
          path="/service-accounts"
          render={(routeProps) => <ServiceAccounts routeProps={routeProps} />}
        />
        <Route
          path="/safe"
          render={(routeProps) => <SafePageLayout routeProps={routeProps} />}
        />
        <Route path="/home" render={() => <Home />} />

        <Redirect exact to="/" />
      </Switch>
    </Suspense>
  );
};

export default PrivateRoutes;
