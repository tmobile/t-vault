/* eslint-disable import/no-unresolved */
import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

const ChildItemWrap = styled('div')``;
const AddForm = (props) => {
  const { inputNode, createButton, inputEnabled } = props;
  return (
    <ComponentError>
      <ChildItemWrap>
        {inputEnabled ? inputNode : <></>}
        {!inputEnabled ? createButton : <></>}
      </ChildItemWrap>
    </ComponentError>
  );
};
AddForm.propTypes = {
  inputNode: PropTypes.node,
  createButton: PropTypes.node,
  inputEnabled: PropTypes.bool,
};
AddForm.defaultProps = {
  inputNode: <div />,
  createButton: <div />,
  inputEnabled: false,
};
export default AddForm;
