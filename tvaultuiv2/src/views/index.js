/* eslint-disable react/jsx-wrap-multilines */
import React, { Suspense, lazy } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';
import styled from 'styled-components';

import Safe from './private/safe';
import ScaledLoader from '../components/Loaders/ScaledLoader';
import { UserContextProvider } from '../contexts';

const Home = lazy(() => import('./public/HomePage'));
const VaultAppRoles = lazy(() => import('./private/vault-app-roles'));
const Certificates = lazy(() => import('./private/certificates'));
const ServiceAccounts = lazy(() => import('./private/service-accounts'));

const LoaderWrap = styled('div')`
  height: 100vh;
  display: flex;
  justify-content: center;
  align-item: center;
`;

const Wrapper = styled.section`
  max-width: 130rem;
  margin: auto;
`;

const PrivateRoutes = () => {
  return (
    <UserContextProvider>
      <Suspense
        fallback={
          <LoaderWrap>
            <ScaledLoader />
          </LoaderWrap>
        }
      >
        <Switch>
          <Redirect exact from="/" to="/safes" />
          <Route
            path="/vault-app-roles"
            render={(routeProps) => (
              <Wrapper>
                <VaultAppRoles routeProps={routeProps} />
              </Wrapper>
            )}
          />
          <Route
            path="/certificates"
            render={(routeProps) => (
              <Wrapper>
                <Certificates routeProps={routeProps} />
              </Wrapper>
            )}
          />
          <Route
            path="/service-accounts"
            render={(routeProps) => (
              <Wrapper>
                <ServiceAccounts routeProps={routeProps} />
              </Wrapper>
            )}
          />
          <Route
            path="/safes"
            render={(routeProps) => (
              <Wrapper>
                <Safe routeProps={routeProps} />
              </Wrapper>
            )}
          />
          <Route path="/home" render={() => <Home />} />
          <Redirect exact to="/" />
        </Switch>
      </Suspense>
    </UserContextProvider>
  );
};

export default PrivateRoutes;
