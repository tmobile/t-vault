/* eslint-disable no-nested-ternary */
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
import useMediaQuery from '@material-ui/core/useMediaQuery';
import Error from 'components/Error';
import Loader from 'components/Loader';
import ButtonComponent from '../../../../../components/FormFields/ActionButton';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import addFolderPlus from '../../../../../assets/folder-plus.svg';
import NoSecretsIcon from '../../../../../assets/no-data-secrets.svg';
import NamedButton from '../../../../../components/NamedButton';
import NoData from '../../../../../components/NoData';
import mediaBreakpoints from '../../../../../breakpoints';
import AddFolder from '../AddFolder';
import Tree from '../Tree';
import Permissions from '../Permissions';
import apiService from '../../apiService';
import SnackbarComponent from '../../../../../components/Snackbar';
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
  ${mediaBreakpoints.small} {
    height: 77vh;
  }
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
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
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

export default function SelectionTabs(props) {
  const { safeDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [enabledAddFolder, setEnableAddFolder] = useState(false);
  const [secretsFolder, setSecretsFolder] = useState([]);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  // const [secrets, setSecrets] = useState([]);

  // resolution handlers
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  // const isDeskTopView = useMediaQuery(mediaBreakpoints.desktop);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };
  const addSecretsFolder = () => {
    setEnableAddFolder(true);
  };
  // toast close handling
  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  const addSecretsFolderList = (secretFolder) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.id = `${secretFolder.parentId}/${secretFolder.value}`;
    folderObj.parentId = secretFolder.parentId;
    folderObj.value = secretFolder.value;
    folderObj.type = secretFolder.type || 'folder';
    folderObj.children = [];
    tempFolders.push(folderObj);
    setResponseType(0);
    apiService
      .addFolder(folderObj.id)
      // eslint-disable-next-line no-unused-vars
      .then((res) => {
        setToastMessage(res.data.messages[0]);
        setSecretsFolder([...tempFolders]);
        setResponseType(1);
      })
      .catch((error) => {
        console.log(error.toString());
        setResponseType(-1);
        if (!error.toString().toLowerCase().includes('network')) {
          setToastMessage(error.response.data.messages[0]);
        }
        setToastMessage('Network Error');
      });
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
          {value === 0 && (
            <NamedButton
              label="Add Folder"
              onClick={addSecretsFolder}
              customStyle={customBtnStyles}
              iconSrc={addFolderPlus}
              disable={safeDetail.access.toLowerCase() === 'read'}
            />
          )}
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
              parentId={safeDetail.path}
              handleCancelClick={() => setEnableAddFolder(false)}
            />
          ) : (
            <></>
          )}

          {responseType === 0 ? (
            <Loader />
          ) : responseType === -1 && !enabledAddFolder ? (
            <EmptySecretBox>
              {' '}
              <Error description="error in creating folder" />
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                severity="error"
                icon="error"
                message={toastMessage || 'Something went wrong!'}
              />
            </EmptySecretBox>
          ) : (
            responseType === 1 &&
            !enabledAddFolder && (
              <SnackbarComponent
                open
                onClose={() => onToastClose()}
                message={toastMessage || 'Request succesfull'}
              />
            )
          )}
          {secretsFolder && secretsFolder.length ? (
            <Tree data={secretsFolder} />
          ) : (
            !enabledAddFolder &&
            responseType !== -1 &&
            responseType !== 0 && (
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
                      disable={safeDetail.access.toLowerCase() === 'read'}
                      width={isMobileScreen ? '100%' : ''}
                      onClick={() => setEnableAddFolder(true)}
                    />
                  }
                  bgIconStyle={bgIconStyle}
                  width={isMobileScreen ? '100%' : '30%'}
                />
              </EmptySecretBox>
            )
          )}
        </TabPanel>
        <TabPanel value={value} index={1}>
          <Permissions />
        </TabPanel>
      </div>
    </ComponentError>
  );
}
SelectionTabs.propTypes = {
  safeDetail: PropTypes.object,
};
SelectionTabs.defaultProps = {
  safeDetail: {},
};
