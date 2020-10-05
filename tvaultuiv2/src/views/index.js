/* eslint-disable react/jsx-wrap-multilines */
import React, { Suspense, lazy } from 'react';
import {
  BrowserRouter as Router,
  Route,
  Switch,
  Redirect,
} from 'react-router-dom';
import styled from 'styled-components';

import Safe from './private/safe';
import ScaledLoader from '../components/Loaders/ScaledLoader';

const Home = lazy(() => import('./public/HomePage'));
const Applications = lazy(() => import('./private/applications'));
const Certificates = lazy(() => import('./private/certificates'));
const ServiceAccounts = lazy(() => import('./private/service-accounts'));

const LoaderWrap = styled('div')`
  height: calc(100vh - 7rem);
  display: flex;
  justify-content: center;
  align-item: center;
`;

const PrivateRoutes = () => {
  return (
    <Suspense
      fallback={
        <LoaderWrap>
          <ScaledLoader />
        </LoaderWrap>
      }
    >
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
