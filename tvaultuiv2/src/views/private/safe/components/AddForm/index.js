import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';

const ChildItemWrap = styled('div')``;
const AddForm = (props) => {
  const { inputNode, inputEnabled } = props;
  return (
    <ComponentError>
      <ChildItemWrap>{inputEnabled ? inputNode : <></>}</ChildItemWrap>
    </ComponentError>
  );
};
AddForm.propTypes = {
  inputNode: PropTypes.node,
  inputEnabled: PropTypes.bool,
};
AddForm.defaultProps = {
  inputNode: <div />,
  inputEnabled: false,
};
export default AddForm;
