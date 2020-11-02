import React from 'react';
import { withRouter } from 'react-router-dom';
import AppRolesDashboard from './components/AppRolesDashboard';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import { SectionPreview } from '../../../styles/GlobalStyles';

const VaultAppRoles = () => {
  return (
    <ComponentError>
      <SectionPreview>
        <AppRolesDashboard />
      </SectionPreview>
    </ComponentError>
  );
};

VaultAppRoles.propTypes = {};

VaultAppRoles.defaultProps = {};

export default withRouter(VaultAppRoles);
