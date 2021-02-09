/* eslint-disable no-use-before-define */
/* eslint-disable react/jsx-wrap-multilines */
import React, { Suspense, lazy, useState, useEffect } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';
import styled from 'styled-components';
import { useIdleTimer } from 'react-idle-timer';
import * as msal from '@azure/msal-browser';
import Safe from './private/safe';
import ScaledLoader from '../components/Loaders/ScaledLoader';
import { UserContextProvider } from '../contexts';
import { revokeToken, renewToken } from './public/HomePage/utils';
import configData from '../config/config';
import configUrl from '../config/index';

const Home = lazy(() => import('./public/HomePage'));
const VaultAppRoles = lazy(() => import('./private/vault-app-roles'));
const Certificates = lazy(() => import('./private/certificates'));
const ServiceAccounts = lazy(() => import('./private/service-accounts'));
const IamServiceAccounts = lazy(() => import('./private/iam-service-accounts'));
const AzurePrincipal = lazy(() => import('./private/azureprincipal'));

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
  const [idleTimer] = useState(1000 * 60 * 3);
  const [timeWhenLoggedIn, setTimeWhenLoggedIn] = useState(
    new Date().getTime()
  );
  const [startingMinutes, setStartingMinutes] = useState(27);

  let timerVal = startingMinutes * 60;

  const countDownTimer = () => {
    let timeStamp;
    const initCountdown = () => {
      timeStamp = setInterval(() => {
        const minutes = Math.floor(timerVal / 60);
        let seconds = timerVal % 60;
        seconds = seconds < 10 ? `0${seconds}` : seconds;
        if (timerVal === 0) {
          loggedOut();
        } else {
          document.title = `${minutes}:${seconds} until your session timeout!`;
          timerVal -= 1;
        }
      }, 1000);
    };
    const cancelCountdown = () => {
      clearInterval(timeStamp);
    };
    return { initCountdown, cancelCountdown };
  };

  const timer = countDownTimer();

  useEffect(() => {
    return () => {
      timer.cancelCountdown();
    };
  }, [timer]);

  const loggedOut = async () => {
    document.title = 'Your session has expired.';
    timer.cancelCountdown();
    if (configData.AUTH_TYPE === 'oidc') {
      const config = {
        auth: {
          clientId: sessionStorage.getItem('clientId'),
          redirectUri: configUrl.redirectUrl, // defaults to application start page
          postLogoutRedirectUri: configUrl.redirectUrl,
        },
      };

      const myMsal = new msal.PublicClientApplication(config);
      myMsal.logout();
      await revokeToken();
    }
    window.location.href = '/';
    sessionStorage.clear();
  };

  const handleOnIdle = () => {
    if (window.location.pathname !== '/' && configData.AUTH_TYPE === 'oidc') {
      timer.initCountdown();
    }
  };

  const callRenewApi = async () => {
    setStartingMinutes(27);
    try {
      await renewToken();
    } catch (err) {
      loggedOut();
    }
  };

  // when idle
  const handleOnActive = async () => {
    if (window.location.pathname !== '/' && configData.AUTH_TYPE === 'oidc') {
      if (getRemainingTime() === 0) {
        document.title = 'VAULT';
        timer.cancelCountdown();
        setTimeWhenLoggedIn(new Date().getTime());
        await callRenewApi();
      }
    }
  };

  const handleOnAction = async () => {
    if (window.location.pathname !== '/') {
      const lastActive = getLastActiveTime();
      const lastIdle = getLastIdleTime();
      const diffBetweenLastIdle = (lastActive - lastIdle) / 60000;
      const diff = Math.abs(timeWhenLoggedIn - lastActive);
      const minutes = diff / 60000;
      if (configData.AUTH_TYPE !== 'oidc' && minutes > 30) {
        loggedOut();
      } else if (
        configData.AUTH_TYPE === 'oidc' &&
        minutes > 27 &&
        diffBetweenLastIdle > 30
      ) {
        setTimeWhenLoggedIn(new Date().getTime());
        await callRenewApi();
      }
    }
  };

  const { getRemainingTime, getLastActiveTime, getLastIdleTime } = useIdleTimer(
    {
      timeout: idleTimer,
      onIdle: handleOnIdle,
      onActive: handleOnActive,
      onAction: handleOnAction,
    }
  );

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
            path="/iam-service-accounts"
            render={(routeProps) => (
              <Wrapper>
                <IamServiceAccounts routeProps={routeProps} />
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
          <Route
            path="/azure-principal"
            render={(routeProps) => (
              <Wrapper>
                <AzurePrincipal routeProps={routeProps} />
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
