/* eslint-disable import/no-unresolved */
/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
// eslint-disable-next-line react/forbid-prop-types
// eslint-disable-next-line react/require-default-props
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ButtonComponent from 'components/FormFields/ActionButton';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import addFolderPlus from 'assets/folder-plus.svg';
import NoSecretsIcon from 'assets/no-data-secrets.svg';
// eslint-disable-next-line import/no-unresolved
import NamedButton from 'components/NamedButton';
import NoData from 'components/NoData';
import AddFolder from '../AddFolder';
// import FolderTreeView from '../FolderTree';
import Tree from '../Tree';
// styled components goes here

const EmptySecretBox = styled('div')`
  width: 100%;
  position: absolute;
  display: flex;
  justify-content: center;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
`;
const customBtnStyles = css`
  padding: 0.2rem 1rem;
  border-radius: 0.5rem;
`;

const TabPanelWrap = styled.div`
  height: 54.75vh;
  position: relative;
  overflow: auto;
  margin-top: 1.3rem;
`;
const bgIconStyle = {
  width: '16rem',
  height: '16rem',
};
const CountSpan = styled.span`
  margin-top: 1.5rem;
  color: #5e627c;
  font-size: 1.3rem;
`;

const TabPanel = (props) => {
  const { children = '', value, index } = props;

  return (
    <TabPanelWrap
      role="tabpanel"
      hidden={value !== index}
      id={`safes-tabpanel-${index}`}
      aria-labelledby={`safe-tab-${index}`}
    >
      {children}
    </TabPanelWrap>
  );
};

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.any.isRequired,
  value: PropTypes.any.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
  // eslint-disable-next-line react/default-props-match-prop-types
  index: PropTypes.any.isRequired,
  // eslint-disable-next-line react/default-props-match-prop-types
  value: PropTypes.any.isRequired,
};

function a11yProps(index) {
  return {
    id: `safety-tab-${index}`,
    'aria-controls': `safety-tabpanel-${index}`,
  };
}

const useStyles = makeStyles(() => ({
  root: {
    flexGrow: 1,
    padding: '0 2.1rem',
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  tab: {
    minWidth: '9.5rem',
  },
}));

export default function SelectionTabs() {
  // const { secrets } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [enabledAddFolder, setEnableAddFolder] = useState(false);
  const [secretsFolder, setSecretsFolder] = useState([]);
  // const [secrets, setSecrets] = useState([]);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };
  const addSecretsFolder = () => {
    setEnableAddFolder(true);
  };

  const addSecretsFolderList = (name) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.labelText = name;
    folderObj.type = name.type || 'folder';
    folderObj.children = [];
    tempFolders.push(folderObj);
    setSecretsFolder([...tempFolders]);
    setEnableAddFolder(false);
  };
  /**
   *Creates secrets folder array
   * @param {string} folderName
   */
  return (
    <ComponentError>
      <div className={classes.root}>
        <AppBar position="static" className={classes.appBar}>
          <Tabs
            value={value}
            onChange={handleChange}
            aria-label="safe tabs"
            indicatorColor="secondary"
            textColor="primary"
          >
            {/* eslint-disable-next-line react/jsx-props-no-spreading */}
            <Tab className={classes.tab} label="Secrets" {...a11yProps(0)} />
            {/* eslint-disable-next-line react/jsx-props-no-spreading */}
            <Tab label="Permissions" {...a11yProps(1)} />
          </Tabs>
          <NamedButton
            label="Add Folder"
            onClick={addSecretsFolder}
            customStyle={customBtnStyles}
            iconSrc={addFolderPlus}
          />
        </AppBar>
        <TabPanel value={value} index={0}>
          {
            <CountSpan color="#5e627c">
              {`${secretsFolder && secretsFolder.length} Secrets`}
            </CountSpan>
          }
          {enabledAddFolder ? (
            <AddFolder
              handleSaveClick={addSecretsFolderList}
              handleCancelClick={() => setEnableAddFolder(false)}
            />
          ) : (
            <></>
          )}
          {secretsFolder && secretsFolder.length ? (
            <>
              <Tree data={secretsFolder} />
            </>
          ) : (
            !enabledAddFolder && (
              <EmptySecretBox>
                <NoData
                  imageSrc={NoSecretsIcon}
                  description="add a <strong>Folder</strong> and then you will be able to add <strong>secrets</strong> to view them all here"
                  actionButton={
                    // eslint-disable-next-line react/jsx-wrap-multilines
                    <ButtonComponent
                      label="add"
                      icon="add"
                      color="secondary"
                      onClick={() => setEnableAddFolder(true)}
                    />
                  }
                  bgIconStyle={bgIconStyle}
                  width="30%"
                />
              </EmptySecretBox>
            )
          )}
        </TabPanel>
        <TabPanel value={value} index={1}>
          Permissions
        </TabPanel>
      </div>
    </ComponentError>
  );
}
SelectionTabs.propTypes = {
  secrets: PropTypes.array,
};
SelectionTabs.defaultProps = {
  secrets: [],
};
