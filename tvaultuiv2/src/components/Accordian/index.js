import React, { useState } from 'react';
import PropTypes from 'prop-types';

import styled from 'styled-components';
import Accordion from '@material-ui/core/Accordion';
import AccordionSummary from '@material-ui/core/AccordionSummary';
import AccordionDetails from '@material-ui/core/AccordionDetails';
import FolderOutlinedIcon from '@material-ui/icons/FolderOutlined';
import MoreVertOutlinedIcon from '@material-ui/icons/MoreVertOutlined';
import CreateSecret from '../../views/private/safe/components/CreateSecrets';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import AddFolder from '../../views/private/safe/components/AddFolder';
import PopperElement from '../../views/private/safe/components/Popper';
import CreateSecretButton from '../../views/private/safe/components/CreateSecretButton';

// styled components goes here
const FolderWrap = styled(Accordion)`
  display: flex;
  position: relative;
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
    justify-content: space-between;
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
  const {
    summaryIcon,
    title,
    date,
    accordianChildren,
    saveSecretsToFolder,
    addFolderToTargetFolder,
    handleCancelClick,
  } = props;
  const [addSecretEnabled, setAddSecretEnabled] = useState(false);
  const [isPopperOpen, setIsPopperOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const [isAddFolder, setIsAddFolder] = useState(false);

  const handleSaveSecretsToFolder = (secret) => {
    saveSecretsToFolder(secret);
    setAddSecretEnabled(false);
  };

  const enablePopper = (e) => {
    setIsPopperOpen(true);
    setAnchorEl(e.currentTarget);
  };

  const handlePopperClick = () => {
    addFolderToTargetFolder();
    setIsAddFolder(true);
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
            <FolderOutlinedIcon />
            <SecretTitleWrap>
              <PElement>{title}</PElement>
              <PElement>{date}</PElement>
            </SecretTitleWrap>
          </FolderIconWrap>
          <FolderIconWrap onClick={(e) => enablePopper(e)}>
            <MoreVertOutlinedIcon />
            <PopperElement
              open={isPopperOpen}
              popperContent={<span>create folder</span>}
              position="right-start"
              anchorEl={anchorEl}
              handlePopperClick={handlePopperClick}
            />
          </FolderIconWrap>
        </FolderSummary>
        <SecretDetail>
          <SecretsListWrapper>{accordianChildren}</SecretsListWrapper>
          {isAddFolder ? (
            <AddFolder
              handleCancelClick={handleCancelClick}
              handleSaveClick={saveSecretsToFolder}
            />
          ) : (
            <></>
          )}
          {addSecretEnabled ? (
            <CreateSecret
              handleSecretSave={handleSaveSecretsToFolder}
              handleSecretCancel={setAddSecretEnabled}
            />
          ) : (
            <CreateSecretButton
              onClickHandler={() => setAddSecretEnabled(true)}
            />
          )}
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
  accordianChildren: PropTypes.node,
  saveSecretsToFolder: PropTypes.func,
  addFolderToTargetFolder: PropTypes.func,
  handleCancelClick: PropTypes.func,
};
AccordionFolder.defaultProps = {
  summaryIcon: <div />,
  // titleIcon: <div />,
  title: '',
  date: '',
  accordianChildren: <div />,
  saveSecretsToFolder: () => {},
  addFolderToTargetFolder: () => {},
  handleCancelClick: () => {},
};
export default AccordionFolder;
