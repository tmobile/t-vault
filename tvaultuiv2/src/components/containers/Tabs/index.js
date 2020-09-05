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
import Typography from '@material-ui/core/Typography';
import Box from '@material-ui/core/Box';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import AddFolder from 'components/add-folder';
// import CreateSecret from 'components/createSecret';

// eslint-disable-next-line import/no-unresolved

// import ExpandMoreIcon from '@material-ui/icons/ExpandMore';
// import FolderIcon from '@material-ui/icons/Folder';
import AddIcon from '@material-ui/icons/Add';
// import AccordionFolder from '../../common/AccordionFolder';
import { findElementById } from 'services/helperFunctions';
import MuiButton from '../../common/MuiButton';
import DialogeBox from '../../common/DialogeBox';
import FolderTreeView from '../FolderTree';
// styled components goes here

const EmptySecretBox = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
`;
const customBtnStyles = css`
  padding: 0.2rem 1rem;
  border-radius: 0.5rem;
`;

const TabPanelWrap = styled.div`
  .MuiBox-root {
    padding: 1em 0;
  }
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
      {value === index && (
        <Box p={3}>
          <Typography>{children}</Typography>
        </Box>
      )}
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

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    backgroundColor: theme.palette.background.paper,
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
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

  /**
   *Creates secrets folder array
   * @param {string} folderName
   */

  const saveSecretsToFolder = (obj, parentId) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.labelText = obj.name;
    folderObj.type = obj.type;
    folderObj.labelKey = obj.key;
    folderObj.children = [];
    const selectedItem = findElementById(tempFolders, parentId, 'children');
    selectedItem.children.push(folderObj);
    setSecretsFolder([...tempFolders]);
    setEnableAddFolder(false);
  };

  return (
    <ComponentError>
      <div className={classes.root}>
        <AppBar position="static" className={classes.appBar}>
          <Tabs value={value} onChange={handleChange} aria-label="safe tabs">
            {/* eslint-disable-next-line react/jsx-props-no-spreading */}
            <Tab label="Secrets" {...a11yProps(0)} />
            {/* eslint-disable-next-line react/jsx-props-no-spreading */}
            <Tab label="Permissions" {...a11yProps(1)} />
          </Tabs>
          <MuiButton
            label="ADD"
            onClick={addSecretsFolder}
            customStyle={customBtnStyles}
            icon={<AddIcon />}
          />
        </AppBar>
        <TabPanel value={value} index={0}>
          {enabledAddFolder ? (
            <AddFolder
              handleSaveClick={saveSecretsToFolder}
              handleCancelClick={() => setEnableAddFolder(false)}
            />
          ) : (
            <></>
          )}
          {secretsFolder ? (
            <>
              {/* reverted folder tree structure and its in different branch(layout-branch) */}
              <FolderTreeView
                treeData={secretsFolder}
                saveSecretsToFolder={saveSecretsToFolder}
                handleCancelClick={() => setEnableAddFolder(false)}
              />

              {/* {secretsFolder.map((item) => (
                <AccordionFolder
                  title={item}
                  date="2 days ago"
                  saveSecretsToFolder={saveSecretsToFolder}
                  se={<FolderTreeView treeData={secrets} />}
                />
              ))} */}
            </>
          ) : (
            <EmptySecretBox>
              <DialogeBox
                description="add a <strong>Folder</strong> and then you will be able to add <strong>secrets</strong> to view them all here"
                actionButton={
                  // eslint-disable-next-line react/jsx-wrap-multilines
                  <MuiButton
                    label="ADD"
                    icon={<AddIcon onClickFunc={addSecretsFolder} />}
                  />
                }
              />
            </EmptySecretBox>
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
