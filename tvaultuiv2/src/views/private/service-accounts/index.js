/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import ServiceAccountDashboard from './components/ServiceAccountDashboard';
import { SectionPreview } from '../../../styles/GlobalStyles';

const ServiceAccountLayout = (props) => {
  return (
    <ComponentError>
      <SectionPreview>
        <ServiceAccountDashboard {...props} />
      </SectionPreview>
    </ComponentError>
  );
};
ServiceAccountLayout.propTypes = {};

ServiceAccountLayout.defaultProps = {};
export default withRouter(ServiceAccountLayout);
