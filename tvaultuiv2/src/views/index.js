/* eslint-disable no-unused-vars */
/* eslint-disable no-use-before-define */
/* eslint-disable no-console */
/* eslint-disable react/jsx-wrap-multilines */
import React, { Suspense, lazy, useState } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';
import styled from 'styled-components';
import { useIdleTimer } from 'react-idle-timer';

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
  const [, setIsTimedOut] = useState(false);
  const [idleTimer] = useState(1000 * 60 * 30);

  const handleOnIdle = (event) => {
    console.log('last active time', getLastActiveTime());
    window.location.href = '/';
    sessionStorage.clear();
  };

  const handleOnActive = (event) => {
    console.log('time remaining', getRemainingTime());
    setIsTimedOut(false);
  };

  const handleOnAction = (e) => {
    console.log('user did something', getLastActiveTime());
    setIsTimedOut(false);
  };
  const { getRemainingTime, getLastActiveTime } = useIdleTimer({
    timeout: idleTimer,
    onIdle: handleOnIdle,
    onActive: handleOnActive,
    onAction: handleOnAction,
    debounce: 250,
  });

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
          {/* <Redirect exact from="/" to="/home" /> */}
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
          <Route path="/" render={() => <Home />} />
          <Redirect exact to="/" />
        </Switch>
      </Suspense>
    </UserContextProvider>
  );
};

export default PrivateRoutes;
