/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import PropTypes from 'prop-types';

import styled from 'styled-components';
import Accordion from '@material-ui/core/Accordion';
import AccordionSummary from '@material-ui/core/AccordionSummary';
import AccordionDetails from '@material-ui/core/AccordionDetails';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import FolderIcon from '@material-ui/icons/Folder';
import LockIcon from '@material-ui/icons/Lock';
import CreateSecret from 'views/private/safe/components/CreateSecrets';

// styled components goes here
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
  display: flex;
  flex-direction: column;
  align-items: center;
  //   padding: 1em;
`;
const SecretTitleWrap = styled('div')``;
const PElement = styled('div')`
  margin: 0.5rem 0;
  display: flex;
  align-items: center;
`;
const CreateSecretWrap = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f2f2;
  padding: 0.5em;
  cursor: pointer;
`;
const SpanElement = styled('span')``;
const FolderIconWrap = styled('div')`
  margin: 0 1em;
  display: flex;
  align-items: center;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
  }
`;
const SecretsListWrapper = styled.div`
  width: 100%;
`;

const AccordionFolder = (props) => {
  const { summaryIcon, title, date } = props;
  const [addSecretEnabled, setAddSecretEnabled] = useState(false);
  const [secrets, setSecrets] = useState([]);

  const saveSecretsToFolder = (secret) => {
    const tempSecretsList = [...secrets] || [];
    tempSecretsList.push(secret);
    setSecrets([...tempSecretsList]);
    setAddSecretEnabled(false);
  };
  return (
    <ComponentError>
      <FolderWrap>
        <FolderSummary
          expandIcon={summaryIcon}
          aria-controls="panel1a-content"
          id="panl1a-header"
        >
          {' '}
          <FolderIconWrap>
            <FolderIcon />
          </FolderIconWrap>
          <SecretTitleWrap>
            <PElement>{title}</PElement>
            <PElement>{date}</PElement>
          </SecretTitleWrap>
        </FolderSummary>
        <SecretDetail>
          {addSecretEnabled ? (
            <CreateSecret
              handleSecretSave={saveSecretsToFolder}
              handleSecretCancel={setAddSecretEnabled}
            />
          ) : (
            <CreateSecretWrap onClick={() => setAddSecretEnabled(true)}>
              <SpanElement>+</SpanElement>
              <SpanElement>Create secrets</SpanElement>
            </CreateSecretWrap>
          )}
          <SecretsListWrapper>
            {secrets.map((secret) => (
              <PElement>
                <LockIcon />
                <SpanElement>{secret}</SpanElement>
              </PElement>
            ))}
          </SecretsListWrapper>
        </SecretDetail>
      </FolderWrap>
    </ComponentError>
  );
};
AccordionFolder.propTypes = {
  summaryIcon: PropTypes.node,
  // titleIcon: PropTypes.node,
  title: PropTypes.string,
  date: PropTypes.string,
};
AccordionFolder.defaultProps = {
  summaryIcon: <div />,
  // titleIcon: <div />,
  title: 'Nothing here',
  date: 'No Date',
};
export default AccordionFolder;
