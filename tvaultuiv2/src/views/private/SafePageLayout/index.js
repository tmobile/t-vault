/* eslint-disable react/jsx-props-no-spreading */
import React from 'react';
import { withRouter } from 'react-router-dom';

// import PropTypes from 'prop-types';
// eslint-disable-next-line import/no-unresolved
import SafeSectionWrap from 'components/containers/SafeSectionWrap';
import styled from 'styled-components';

const SafeSectionPreview = styled('section')`
  border: 2px solid #ccc;
  width: 80%;
  margin: 3em auto;
`;

const SafePageLayout = (props) => {
  return (
    <main title="safe-layout">
      <SafeSectionPreview>
        <SafeSectionWrap {...props} />
      </SafeSectionPreview>
    </main>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
