/* eslint-disable import/no-unresolved */
import React from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ReactHtmlParser from 'react-html-parser';
import ComponentError from 'errorBoundaries/ComponentError/component-error';

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
    <ComponentError>
      <DialogeBoxWrapper>
        <BoxDescription>{ReactHtmlParser(description)}</BoxDescription>
        <ActionButtonWrap>{actionButton}</ActionButtonWrap>
      </DialogeBoxWrapper>
    </ComponentError>
  );
};
DialogeBox.propTypes = {
  description: PropTypes.string,
  actionButton: PropTypes.node,
};
DialogeBox.defaultProps = {
  description: 'Nothing here, But me',
  actionButton: <div />,
};
export default DialogeBox;
