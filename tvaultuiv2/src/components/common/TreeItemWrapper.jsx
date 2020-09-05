import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

const ChildItemWrap = styled('div')``;
const TreeItemWrapper = (props) => {
  const { inputNode, createButton, renderChilds, inputEnabled } = props;
  return (
    <ChildItemWrap>
      {inputEnabled ? inputNode : <></>}
      {createButton}
      {renderChilds}
    </ChildItemWrap>
  );
};
TreeItemWrapper.propTypes = {
  inputNode: PropTypes.node,
  createButton: PropTypes.node,
  renderChilds: PropTypes.node,
  inputEnabled: PropTypes.bool,
};
TreeItemWrapper.defaultProps = {
  inputNode: <div />,
  createButton: <div />,
  renderChilds: <div />,
  inputEnabled: false,
};
export default TreeItemWrapper;
