/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import IamServiceAccountDashboard from './components/IamServiceAccountDashboard';
import { SectionPreview } from '../../../styles/GlobalStyles';

const IamServiceAccountLayout = (props) => {
  return (
    <ComponentError>
      <SectionPreview>
        <IamServiceAccountDashboard {...props} />
      </SectionPreview>
    </ComponentError>
  );
};
IamServiceAccountLayout.propTypes = {};

IamServiceAccountLayout.defaultProps = {};
export default withRouter(IamServiceAccountLayout);
