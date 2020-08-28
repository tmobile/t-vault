import React, { Suspense } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';

import SafePageLayout from './SafePageLayout';

const PrivateRoutes = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Switch>
        <Redirect exact from="/private" to="/private/:name" />

        <Route
          path="/private/:name"
          render={(routeProps) => <SafePageLayout routeProps={routeProps} />}
        />
        <Redirect to="/private" />
      </Switch>
    </Suspense>
  );
};

export default PrivateRoutes;
