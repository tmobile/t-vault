import React from 'react';
import PropTypes from 'prop-types';

import styled from 'styled-components';
import Accordion from '@material-ui/core/Accordion';
import AccordionSummary from '@material-ui/core/AccordionSummary';
import AccordionDetails from '@material-ui/core/AccordionDetails';
// import { Avatar } from '@material-ui/core';

//styled components goes here
const FolderWrap = styled(Accordion)`
  display: flex;
  background: #ddd;
  padding: 0.25em;
  justify-content: center;
  flex-direction: column;
  margin-bottom: 1em;
`;
const FolderSummary = styled(AccordionSummary)`
  display: flex;
  width: 100%;
  flex-direction: row-reverse;
  padding: 0;
  .MuiAccordionSummary-content {
    align-items: center;
    margin: 0.5em 0;
  }
`;
const SecretDetail = styled(AccordionDetails)`
  //   padding: 1em;
`;
const SecretTitleWrap = styled('div')``;
const PElement = styled('p')`
  margin: 0;
`;
// const IconWrap = styled('div')``;
const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
  }
`;

const AccordionFolder = (props) => {
  const { summaryIcon, titleIcon, title, date } = props;
  return (
    <FolderWrap>
      <FolderSummary
        expandIcon={summaryIcon}
        aria-controls="panel1a-content"
        id="panl1a-header"
      >
        {' '}
        <FolderIconWrap>{titleIcon}</FolderIconWrap>
        <SecretTitleWrap>
          <PElement>{title}</PElement>
          <PElement>{date}</PElement>
        </SecretTitleWrap>
      </FolderSummary>
      <SecretDetail>
        <PElement>{title}</PElement>
        {/* <SpanElement>{date}</SpanElement>
        <SpanElement>{title}</SpanElement>
        <SpanElement>{date}</SpanElement> */}
      </SecretDetail>
    </FolderWrap>
  );
};
AccordionFolder.propTypes = {
  summaryIcon: PropTypes.node,
  titleIcon: PropTypes.node,
  title: PropTypes.string,
  date: PropTypes.string,
};
AccordionFolder.defaultProps = {
    summaryIcon: <div></div>,
    titleIcon: <div></div>,
    title: "Nothing here",
    date: "No Date",
  };
export default AccordionFolder;
