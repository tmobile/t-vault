import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';

const TooltipText = styled.span`
  visibility: hidden;
  background-color: #fff;
  color: #000;
  white-space: nowrap;
  text-align: center;
  border-radius: 6px;
  padding: 0.5rem 1rem;
  position: absolute;
  z-index: 1;
  font-size: 1.4rem;
  font-weight: bold;
  right: ${(props) => (props.Transfer === 'Transfer' ? '120%' : (props.certificate === 'top' ? '':'94%'))};
  top: ${props => props.certificate === 'top' ? '110%' : '1%'};
  min-width: 7rem;
  ::after {
    content: ' ';
    position: absolute;
    bottom: 30%;
    right: ${props => props.certificate === 'top' ? '' : '-14%'};
    bottom: ${props => props.certificate === 'top' ? '100%' : ''};
    margin-left:  ${props => props.certificate === 'top' ? '-50%' : '-5px'};
    border-width: 5px;
    border-style: solid;
    border-color: ${props=>props.certificate !== 'top' ? 'transparent transparent transparent #fff' : 'transparent transparent #fff transparent'}
  }
`;

const Tooltip = styled.div`
  position: relative;
  :hover ${TooltipText} {
    visibility: visible;
  }
`;

const TooltipComponent = (props) => {
  const { title, renderContent,certificate } = props;

  return (
    <Tooltip>
      {renderContent}
      <TooltipText Transfer={title} certificate={certificate}>{title}</TooltipText>
    </Tooltip>
  );
};

TooltipComponent.propTypes = {
  title: PropTypes.string.isRequired,
  renderContent: PropTypes.node.isRequired,
  certificate: PropTypes.string,
};

TooltipComponent.defaultProps = {
  certificate: '',
}

export default TooltipComponent;
