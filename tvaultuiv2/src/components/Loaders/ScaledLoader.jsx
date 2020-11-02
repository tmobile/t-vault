import React from 'react';
import PropTypes from 'prop-types';
import styled, { keyframes } from 'styled-components';

// styled components
const LoaderContainer = styled('div')`
  display: flex;
  align-items: center;
  width: ${(props) => props.width};
  height: ${(props) => props.height};
  justify-content: center;
  position: absolute;
`;
const loaderRotate = keyframes`
0% {
  -webkit-transform: rotate(0deg) scale(1);
          transform: rotate(0deg) scale(1); }
50% {
  -webkit-transform: rotate(180deg) scale(0.6);
          transform: rotate(180deg) scale(0.6); }
100% {
  -webkit-transform: rotate(360deg) scale(1);
          transform: rotate(360deg) scale(1); }
`;

const loaderScale = keyframes` 
  0% {
    -webkit-transform: scale(1);
            transform: scale(1);
    opacity: 1; }
  45% {
    -webkit-transform: scale(0.1);
            transform: scale(0.1);
    opacity: 0.7; }
  80% {
    -webkit-transform: scale(1);
            transform: scale(1);
    opacity: 1; } `;

const LoaderWrap = styled('div')`
  position: relative;
  -webkit-transform: translateY(-1.5rem);
  transform: translateY(-1.5rem);
  & > div {
    -webkit-animation-fill-mode: both;
    animation-fill-mode: both;
    position: absolute;
    top: 0px;
    left: 0px;
    border-radius: 100%;
  }
  & > div:first-child {
    background: #e20074;
    width: 2rem;
    height: 2rem;
    left: -0.9rem;
    top: 0.5rem;
    -webkit-animation: ${loaderScale} 1s 0s cubic-bezier(0.09, 0.57, 0.49, 0.9)
      infinite;
    animation: ${loaderScale} 1s 0s cubic-bezier(0.09, 0.57, 0.49, 0.9) infinite;
  }
  & > div:last-child {
    position: absolute;
    border: 0.2rem solid #e20074;
    width: 4rem;
    height: 4rem;
    left: -1.9rem;
    top: -0.5rem;
    background: transparent;
    border: 0.4rem solid;
    border-color: #fff transparent #fff transparent;
    -webkit-animation: ${loaderRotate} 1s 0s cubic-bezier(0.09, 0.57, 0.49, 0.9)
      infinite;
    animation: ${loaderRotate} 1s 0s cubic-bezier(0.09, 0.57, 0.49, 0.9)
      infinite;
    -webkit-animation-duration: 1s;
    animation-duration: 1s;
  }
`;

const Loader = (props) => {
  const { contentWidth, contentHeight } = props;
  return (
    <LoaderContainer width={contentWidth} height={contentHeight}>
      <LoaderWrap>
        <div />
        <div />
      </LoaderWrap>
    </LoaderContainer>
  );
};

Loader.propTypes = {
  contentWidth: PropTypes.string,
  contentHeight: PropTypes.string,
};
Loader.defaultProps = {
  contentWidth: '100%',
  contentHeight: '100%',
};

export default Loader;
