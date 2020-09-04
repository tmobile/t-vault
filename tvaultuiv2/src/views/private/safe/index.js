/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';

// import PropTypes from 'prop-types';
// eslint-disable-next-line import/no-unresolved
import SafeSectionWrap from 'components/containers/SafeSectionWrap';
import styled from 'styled-components';
// eslint-disable-next-line import/no-unresolved
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import apiService from './apiService';

const SafeSectionPreview = styled('section')`
  border: 2px solid #ccc;
  // width: 80%;
  margin: 3em auto;
`;

const SafePageLayout = (props) => {
  // Sample API call. For integration, call like this with you mock data being passed as parameter
  apiService
    .fetchSafe({ name: 'Jack' })
    // eslint-disable-next-line no-console
    .then((res) => console.log('res ---- ', res))
    // eslint-disable-next-line no-console
    .catch((e) => console.error(e));
  return (
    <ComponentError>
      {' '}
      <main title="safe-layout">
        <SafeSectionPreview>
          <SafeSectionWrap {...props} />
        </SafeSectionPreview>
      </main>
    </ComponentError>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
