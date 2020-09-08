/* eslint-disable react/jsx-props-no-spreading */
import React, { lazy, useState } from 'react';
import { Route, Switch, withRouter } from 'react-router-dom';
// import PropTypes from 'prop-types';
// eslint-disable-next-line import/no-unresolved
import styled from 'styled-components';
// eslint-disable-next-line import/no-unresolved

import apiService from './apiService';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import SafeDashboard from './components/SafeDashboard';

const CreateSafe = lazy(() => import('./CreateSafe'));
const SafeSectionPreview = styled('section')`
  margin: 3em auto;
  height: 77vh;
`;

const SafePageLayout = (props) => {
  const { safesList, setSafesList } = useState([]);
  // Sample API call. For integration, call like this with you mock data being passed as parameter
  apiService
    .fetchSafe({ name: 'Jack' })
    // eslint-disable-next-line no-console
    .then((res) => console.log('res ---- ', res))
    // eslint-disable-next-line no-console
    .catch((e) => console.error(e));

  const createSafe = (safeData) => {
    apiService
      .createSafe(safeData)
      .then((res) => {
        setSafesList(res.data);
      })
      .catch((e) => console.log(e));
  };
  return (
    <ComponentError>
      <main title="safe-layout">
        <SafeSectionPreview>
          <SafeDashboard {...props} safesList={safesList} />
        </SafeSectionPreview>
        <Switch>
          <Route
            path="/safe/create-safe"
            render={(routeProps) => (
              <CreateSafe routeProps={routeProps} createSafe={createSafe} />
            )}
          />
        </Switch>
      </main>
    </ComponentError>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
