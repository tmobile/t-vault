/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import { BackgroundColor } from 'styles/GlobalStyles';

const CreateSecretWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${BackgroundColor.secretBg};
  padding: 0.5em;
  cursor: pointer;
`;
const SpanElement = styled.span``;

const CreateSecretButton = (props) => {
  const { onClick } = props;
  return (
    <ComponentError>
      {' '}
      <CreateSecretWrap onClick={() => onClick(true)}>
        <SpanElement>+</SpanElement>
        <SpanElement>Create Secrets</SpanElement>
      </CreateSecretWrap>
    </ComponentError>
  );
};

CreateSecretButton.propTypes = {
  onClickHandler: PropTypes.func,
};
CreateSecretButton.defaultProps = {
  onClickHandler: () => {},
};

export default CreateSecretButton;
