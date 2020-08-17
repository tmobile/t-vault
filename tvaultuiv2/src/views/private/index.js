import React, { Suspense } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';

import SamplePage from './SamplePage';

const PrivateRoutes = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Switch>
        <Redirect exact from="/private" to="/private/dashboard" />
        <Route
          path="/private/dashboard"
          render={(routeProps) => <SamplePage routeProps={routeProps} />}
        />
        <Redirect to="/private" />
      </Switch>
    </Suspense>
  );
};

export default PrivateRoutes;
