/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const CreateSecretWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ddd;
  padding: 0.5em;
  cursor: pointer;
`;
const SpanElement = styled.span``;

const CreateSecretButton = (props) => {
  const { onClickHandler } = props;
  return (
    <ComponentError>
      {' '}
      <CreateSecretWrap onClick={() => onClickHandler(true)}>
        <SpanElement>+</SpanElement>
        <SpanElement>Create secrets</SpanElement>
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
