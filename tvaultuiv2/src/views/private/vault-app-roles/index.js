import React from 'react';
import PropTypes from 'prop-types';
import { Switch, Route } from 'react-infinite-scroller';
import styled from 'styled-components';
import AppRolesDashboard from './components/AppRolesDashboard';
import CreateAppRole from './components/CreateAppRole';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

// styled components goes here
const SectionPreview = styled.section``;

const AppRoles = (props) => {
  // const { message } = props;
  return (
    <ComponentError>
      <SectionPreview>
        <AppRolesDashboard />
      </SectionPreview>
      <Switch>
        <Route
          exact
          path="/vault-app-roles/create-vault-app-role"
          render={(routeProps) => (
            <CreateAppRole routeProps={{ ...routeProps }} />
          )}
        />
        <Route
          exact
          path="/vault-app-roles/edit-vault-app-role"
          render={(routeProps) => (
            <CreateAppRole routeProps={{ ...routeProps }} />
          )}
        />
      </Switch>
    </ComponentError>
  );
};

AppRoles.propTypes = {};

AppRoles.defaultProps = {};

export default AppRoles;
