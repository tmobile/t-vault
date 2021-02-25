import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

const TooltipText = styled.span`
  visibility: hidden;
  background-color: #fff;
  color: #000;
  text-align: center;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  position: absolute;
  z-index: 1;
  font-size: 1.4rem;
  font-weight: bold;
  right: ${(props) => (props.Transfer === 'Transfer' ? '120%' : '94%')};
  top: 1%;
  min-width: 7rem;
  ::after {
    content: ' ';
    position: absolute;
    bottom: 30%;
    right: -14%;
    margin-left: -5px;
    border-width: 5px;
    border-style: solid;
    border-color: transparent transparent transparent #fff;
  }
`;

const Tooltip = styled.div`
  position: relative;
  :hover ${TooltipText} {
    visibility: visible;
  }
`;

const TooltipComponent = (props) => {
  const { title, renderContent } = props;

  return (
    <Tooltip>
      {renderContent}
      <TooltipText Transfer={title}>{title}</TooltipText>
    </Tooltip>
  );
};

TooltipComponent.propTypes = {
  title: PropTypes.string.isRequired,
  renderContent: PropTypes.node.isRequired,
};

export default TooltipComponent;
