import React, { lazy } from 'react';
// import PropTypes from 'prop-types';
import { Switch, Route, withRouter } from 'react-router-dom';
import styled from 'styled-components';
import AppRolesDashboard from './components/AppRolesDashboard';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../../breakpoints';

const CreateAppRole = lazy(() => import('./CreateAppRole'));

// styled components goes here
const SectionPreview = styled('section')`
  margin: 3em auto;
  height: 77vh;
  ${mediaBreakpoints.small} {
    margin: 0;
    height: 89vh;
  }
`;

const VaultAppRoles = (props) => {
  // const { message } = props;
  return (
    <ComponentError>
      <main title="vault-app-role-layout">
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
      </main>
    </ComponentError>
  );
};

VaultAppRoles.propTypes = {};

VaultAppRoles.defaultProps = {};

export default withRouter(VaultAppRoles);
