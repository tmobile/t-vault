import React, { Suspense } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';

import SafePageLayout from './SafePageLayout';
import CreateModal from './CreateSafeModal';

const PrivateRoutes = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Switch>
        <Redirect exact from="/" to="/" />
        <Route
          path="/:name"
          exact
          render={(routeProps) => <SafePageLayout routeProps={routeProps} />}
        />
        <Route
          path="/:name/create-safe"
          render={(routeProps) => <CreateModal routeProps={routeProps} />}
        />
        <Redirect exact to="/" />
      </Switch>
    </Suspense>
  );
};

export default PrivateRoutes;
