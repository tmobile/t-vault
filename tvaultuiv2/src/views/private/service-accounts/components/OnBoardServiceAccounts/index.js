import React from 'react';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';

const Container = styled('div')``;

const onBoardServiceAccount = () => {
  return (
    <ComponentError>
      <Container>service account</Container>;
    </ComponentError>
  );
};
onBoardServiceAccount.propTypes = {};
onBoardServiceAccount.defaultProps = {};
export default onBoardServiceAccount;
