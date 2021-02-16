/* eslint-disable no-console */
/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import { SectionPreview } from '../../../styles/GlobalStyles';
import AzureDashboard from './component/AzureDashboard';

const AzurePrincipal = () => {
  return (
    <ComponentError>
      <SectionPreview>
        <AzureDashboard />
      </SectionPreview>
    </ComponentError>
  );
};
AzurePrincipal.propTypes = {};

AzurePrincipal.defaultProps = {};
export default withRouter(AzurePrincipal);
