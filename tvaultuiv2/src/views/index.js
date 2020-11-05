/* eslint-disable no-use-before-define */
/* eslint-disable react/jsx-wrap-multilines */
import React, { Suspense, lazy, useState } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';
import styled from 'styled-components';
import { useIdleTimer } from 'react-idle-timer';

import Safe from './private/safe';
import ScaledLoader from '../components/Loaders/ScaledLoader';
import { UserContextProvider } from '../contexts';
import { revokeToken, renewToken } from './public/HomePage/utils';
import { addLeadingZeros } from '../services/helper-function';

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
  const [idleTimer, setIdleTimer] = useState(1000 * 60 * 3);
  const [date, setDate] = useState(new Date().getTime());
  const [endTime, setEndTime] = useState(
    new Date(new Date().getTime() + 30 * 60 * 1000)
  );

  const callRenewToken = async () => {
    const renewValue = await renewToken();
    if (renewValue?.data) {
      setIdleTimer(1000 * 60 * 3);
    }
  };

  const calculateCountdown = () => {
    let diff = (Date.parse(endTime) - Date.parse(new Date())) / 1000;
    if (diff <= 0) return false;
    const timeLeft = {
      min: 0,
      sec: 0,
    };
    if (diff >= 60) {
      timeLeft.min = Math.floor(diff / 60);
      diff -= timeLeft.min * 60;
    }
    timeLeft.sec = diff;
    return timeLeft;
  };

  const countDownTimer = () => {
    let timeStamp;
    const initCountdown = () => {
      timeStamp = setInterval(() => {
        const dateVal = calculateCountdown();
        if (dateVal.min === 0 && dateVal.sec <= 1) {
          loggedOut();
        } else if (dateVal !== false) {
          document.title = `${dateVal.min}:${addLeadingZeros(
            dateVal.sec
          )} until your session timeout!`;
        }
      });
    };
    const cancelCountdown = () => {
      clearInterval(timeStamp);
    };
    return { initCountdown, cancelCountdown };
  };

  const timer = countDownTimer();

  const loggedOut = async () => {
    document.title = 'Your session has expired.';
    timer.cancelCountdown();
    await revokeToken();
    window.location.href = '/';
    sessionStorage.clear();
  };

  const handleOnIdle = () => {
    if (window.location.pathname !== '/') {
      timer.initCountdown();
    }
  };

  const handleOnAction = async () => {
    if (window.location.pathname !== '/') {
      timer.cancelCountdown();
      const difference = getLastActiveTime() - date; // This will give difference in milliseconds
      let resultInMinutes = 0;
      resultInMinutes = Math.round(difference / 60000);
      setEndTime(new Date(new Date().getTime() + 30 * 60 * 1000));
      setDate(new Date().getTime());
      if (resultInMinutes > 2) {
        await callRenewToken();
      }
      document.title = 'VAULT';
    }
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
