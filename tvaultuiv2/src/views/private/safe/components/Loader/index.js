import React from 'react';
import styled, { keyframes } from 'styled-components';
import PropTypes from 'prop-types';

// styled components

const animCircle1 = keyframes`
  from {
    transform: rotate(60deg);
  }
  to {
    transform: rotate(205deg);
  }
`;

const animCircle2 = keyframes`
  from {
    transform: rotate(30deg);
  }
  to {
    transform: rotate(-115deg);
  }
`;

const animSpinner = keyframes`
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
`;

const LoaderContainer = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  ${(props) => props.customStyle};
`;

const LoaderWrap = styled.div``;

const Spinner = styled.div`
  display: inline-block;
  animation: ${animSpinner} 0.7s linear infinite;
  .circle {
    width: 32px;
    height: 16px;
    overflow: hidden;
  }
  .circle-inner {
    transform: rotate(45deg);
    border-radius: 50%;
    border: 4px solid #e20074;
    border-right: 4px solid transparent;
    border-bottom: 4px solid transparent;
    width: 100%;
    height: 200%;
    animation: ${animCircle1} 0.7s cubic-bezier(0.25, 0.1, 0.5, 1) infinite;
    animation-direction: alternate;
  }
  .circle-2 {
    transform: rotate(180deg);
    & .circle-inner {
      animation-name: ${animCircle2};
    }
  }
`;
const Loader = (props) => {
  const { customStyle } = props;

  return (
    <LoaderContainer customStyle={customStyle}>
      <LoaderWrap>
        <Spinner>
          <div className="circle circle-1">
            <div className="circle-inner" />
          </div>
          <div className="circle circle-2">
            <div className="circle-inner" />
          </div>
        </Spinner>
      </LoaderWrap>
    </LoaderContainer>
  );
};

Loader.propTypes = {
  customStyle: PropTypes.arrayOf(PropTypes.any),
};

Loader.defaultProps = {
  customStyle: [],
};
export default Loader;
