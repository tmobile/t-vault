import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

const ChildItemWrap = styled('div')``;
const AddForm = (props) => {
  const { inputNode, createButton, inputEnabled } = props;
  return (
    <ChildItemWrap>
      {inputEnabled ? inputNode : <></>}
      {!inputEnabled ? createButton : <></>}
    </ChildItemWrap>
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
