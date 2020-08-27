import React, { Suspense } from 'react';
import { Route, Switch, Redirect } from 'react-router-dom';

import Home from './HomePage';

const PublicRoutes = () => {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <Switch>
        <Redirect exact from="/" to="/home" />
        <Route path="/home" render={() => <Home />} />
        <Redirect to="/home" />
      </Switch>
    </Suspense>
  );
};

export default PublicRoutes;
