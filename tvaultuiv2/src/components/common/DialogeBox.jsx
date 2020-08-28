import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ReactHtmlParser from 'react-html-parser';

const DialogeBoxWrapper = styled('div')`
  display: flex;
  justify-content: center;
  alig-items: center;
  flex-direction: column;
`;
const BoxDescription = styled.p`
  text-align: center;
  font-size: 1.2em;
`;
const ActionButtonWrap = styled('div')`
  display: flexl;
  justify-content: center;
`;
const DialogeBox = (props) => {
  const { description, actionButton } = props;
  return (
    <DialogeBoxWrapper>
      <BoxDescription>{ReactHtmlParser(description)}</BoxDescription>
      <ActionButtonWrap>{actionButton}</ActionButtonWrap>
    </DialogeBoxWrapper>
  );
};
DialogeBox.propTypes = {
  description: PropTypes.string,
  actionButton: PropTypes.node,
};
export default DialogeBox;
