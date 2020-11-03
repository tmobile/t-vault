/* eslint-disable react/jsx-wrap-multilines */
import React, { Suspense, lazy, useState, useEffect, useCallback } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';
import styled from 'styled-components';
import { useIdleTimer } from 'react-idle-timer';

import Safe from './private/safe';
import ScaledLoader from '../components/Loaders/ScaledLoader';
import { UserContextProvider } from '../contexts';
import { revokeToken } from './public/HomePage/utils';
import apiService from './public/HomePage/apiService';

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
  const [idleTimer, setIdleTimer] = useState(1000 * 60 * 30);
  const [date, setDate] = useState(null);

  const renewToken = useCallback(() => {
    if (sessionStorage.getItem('token')) {
      apiService
        .getAuth()
        .then((res) => {
          setDate(new Date().getTime());
          setIdleTimer(res?.data?.lease_duration);
        })
        // eslint-disable-next-line no-console
        .catch((err) => console.log('err', err));
    }
  }, []);

  useEffect(() => {
    renewToken();
  }, [renewToken]);

  const handleOnIdle = async () => {
    await revokeToken();
    window.location.href = '/';
    sessionStorage.clear();
  };

  const handleOnAction = () => {
    // eslint-disable-next-line no-use-before-define
    const difference = getLastActiveTime() - date; // Thiis will give difference in milliseconds
    const resultInMinutes = Math.round(difference / 60000);
    if (resultInMinutes > 3) {
      renewToken();
      setDate(null);
    } else {
      setDate(new Date().getTime());
    }
    setIsTimedOut(false);
  };

  const { getLastActiveTime } = useIdleTimer({
    timeout: idleTimer,
    onIdle: handleOnIdle,
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
