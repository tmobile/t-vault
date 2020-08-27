import React from 'react';
import { withRouter } from 'react-router-dom';

import PropTypes from 'prop-types';
import SafeSectionWrap from 'components/containers/SafeSectionWrap';
import styled from 'styled-components';

const SafeSectionPreview = styled('section')`
  border: 2px solid #ccc;
  width: 80%;
  margin: 3em auto;
`;

const SafePageLayout = () => {
  return (
    <main title="safe-layout">
      <SafeSectionPreview>
        <SafeSectionWrap />
      </SafeSectionPreview>
    </main>
  );
};
SafePageLayout.propTypes = {};

SafePageLayout.defaultProps = {};
export default withRouter(SafePageLayout);
