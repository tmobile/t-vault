/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';
import { SectionPreview } from '../../../styles/GlobalStyles';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import SafeDashboard from './components/SafeDashboard';

const SafePageLayout = (props) => {
  return (
    <ComponentError>
      <SectionPreview>
        <SafeDashboard {...props} />
      </SectionPreview>
    </ComponentError>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
