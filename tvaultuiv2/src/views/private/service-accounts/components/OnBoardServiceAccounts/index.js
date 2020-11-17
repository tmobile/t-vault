import React from 'react';
// import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
// import ScaledLoader from '../../../../../components/Loaders/ScaledLoader';

const Container = styled('div')``;

const OnBoardServiceAccount = () => {
  return (
    <ComponentError>
      <Container>
        <div>Service accounts onboarded</div>
      </Container>
    </ComponentError>
  );
};
OnBoardServiceAccount.propTypes = {};
OnBoardServiceAccount.defaultProps = {};
export default OnBoardServiceAccount;
