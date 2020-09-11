import React, { Suspense, lazy } from 'react';
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect,
} from 'react-router-dom';

import Safe from './private/safe';

const Home = lazy(() => import('./public/HomePage'));
const Applications = lazy(() => import('./private/applications'));
const Certificates = lazy(() => import('./private/certificates'));
const ServiceAccounts = lazy(() => import('./private/service-accounts'));

const PrivateRoutes = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Router>
        <Switch>
          <Redirect exact from="/" to="/safe" />
          <Route
            exact
            path="/applications"
            render={(routeProps) => <Applications routeProps={routeProps} />}
          />
          <Route
            exact
            path="/certificates"
            render={(routeProps) => <Certificates routeProps={routeProps} />}
          />
          <Route
            exact
            path="/service-accounts"
            render={(routeProps) => <ServiceAccounts routeProps={routeProps} />}
          />
          <Route
            path="/safe"
            render={(routeProps) => <Safe routeProps={routeProps} />}
          />
          <Route path="/home" render={() => <Home />} />

          <Redirect exact to="/" />
        </Switch>
      </Router>
    </Suspense>
  );
};

export default PrivateRoutes;
