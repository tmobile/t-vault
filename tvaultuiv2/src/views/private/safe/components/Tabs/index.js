/* eslint-disable no-inner-declarations */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { makeStyles } from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import addFolderPlus from '../../../../../assets/folder-plus.svg';
import NamedButton from '../../../../../components/NamedButton';

import Secrets from '../Secrets';
import mediaBreakpoints from '../../../../../breakpoints';
import { getEachUsersDetails } from '../../../../../services/helper-function';
import Permissions from '../Permissions';
import apiService from '../../apiService';
import AddFolderModal from '../AddFolderModal';
import SnackbarComponent from '../../../../../components/Snackbar';
// styled components goes here

const customBtnStyles = css`
  padding: 0.2rem 1rem;
  border-radius: 0.5rem;
`;

const TabPanelWrap = styled.div`
  position: relative;
  height: 100%;
  margin: 0;
  padding-top: 1.3rem;
  ${mediaBreakpoints.small} {
    height: 77vh;
  }
`;

const TabContentsWrap = styled('div')`
  height: calc(100% - 4.8rem);
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
    height: 'calc( 100% - 19.1rem )',
    display: 'flex',
    flexDirection: 'column',
    background: 'linear-gradient(to bottom,#151820,#2c3040)',
  },
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '4.8rem',
    boxShadow: 'none',
    borderBottom: '0.3rem solid #222632',
  },
  tab: {
    minWidth: '9.5rem',
  },
}));

const SelectionTabs = (props) => {
  const { safeDetail, refresh } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [enabledAddFolder, setEnableAddFolder] = useState(false);
  const [secretsFolder, setSecretsFolder] = useState([]);
  const [safePermissionData, setSafePermissionData] = useState({});
  const [permissionResponseType, setPermissionResponseType] = useState(null);
  const [userHavePermission, setUserHavePermission] = useState({
    permission: false,
    type: '',
  });
  const [response, setResponse] = useState({ status: 'loading' });
  const [userDetails, setUserDetails] = useState([]);
  const [toastResponse, setToastResponse] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
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
    setToastResponse(null);
  };

  const addSecretsFolderList = (secretFolder) => {
    const tempFolders = [...secretsFolder] || [];
    const folderObj = {};
    folderObj.id = `${secretFolder.parentId}/${secretFolder.value}`;
    folderObj.parentId = secretFolder.parentId;
    folderObj.value = secretFolder.value;
    folderObj.type = secretFolder.type || 'folder';
    folderObj.children = [];
    setResponse({ status: 'loading', message: 'loading...' });
    apiService
      .addFolder(folderObj.id)
      .then(() => {
        setResponse({ status: 'success' });
        setToastResponse(1);
        tempFolders[0].children.push(folderObj);
        setSecretsFolder([...tempFolders]);
      })
      .catch((error) => {
        setToastResponse(-1);
        setResponse({ status: 'success' });
        if (error?.response?.data?.errors && error.response.data.errors[0]) {
          setToastMessage(error.response.data.errors[0]);
        }
      });
    setEnableAddFolder(false);
  };

  const getSecretDetails = useCallback(() => {
    if (safeDetail?.access === 'deny') {
      setResponse({ status: 'failed' });
      setSecretsFolder([]);
      return;
    }
    if (safeDetail?.path) {
      setSecretsFolder([]);
      if (!safeDetail.manage) {
        setValue(0);
      }
      setUserHavePermission({
        permission: true,
        type: safeDetail?.access,
      });
      setResponse({ status: 'loading' });
      apiService
        .getSecret(safeDetail.path)
        .then((res) => {
          setResponse({ status: 'success' });
          setSecretsFolder([res.data]);
        })
        .catch(() => {
          setResponse({
            status: 'failed',
            message: 'Network Error',
          });
        });
    } else {
      setResponse({ status: 'success' });
    }
  }, [safeDetail]);

  const fetchPermission = useCallback(() => {
    setPermissionResponseType(0);
    setUserDetails([]);
    setSafePermissionData({});
    return apiService
      .getSafeDetails(`${safeDetail.path}`)
      .then(async (res) => {
        let obj = {};
        if (res && res.data?.data) {
          obj = res.data.data;
          if (res.data.data.users) {
            const eachUsersDetails = await getEachUsersDetails(
              res.data.data.users
            );
            if (eachUsersDetails !== null) {
              setUserDetails([...eachUsersDetails]);
            }
          }
          setSafePermissionData({ response: obj, error: '' });
          setPermissionResponseType(1);
        }
      })
      .catch((err) => {
        setPermissionResponseType(-1);
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          setSafePermissionData({
            response: {},
            error: err.response.data.errors[0],
          });
        }
      });
  }, [safeDetail]);

  useEffect(() => {
    setResponse({ status: 'loading', message: 'loading...' });
    if (safeDetail?.manage) {
      async function fetchData() {
        await fetchPermission();
        getSecretDetails();
      }
      fetchData();
    } else {
      setSafePermissionData({});
      getSecretDetails();
    }
  }, [safeDetail, fetchPermission, getSecretDetails]);

  const safePath = safeDetail?.path;
  useEffect(() => {
    setValue(0);
  }, [safePath]);

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
            <Tab className={classes.tab} label="Secrets" {...a11yProps(0)} />
            {safeDetail.manage && <Tab label="Permissions" {...a11yProps(1)} />}
          </Tabs>
          {value === 0 && safeDetail?.access === 'write' && (
            <NamedButton
              label="Add Folder"
              onClick={addSecretsFolder}
              customStyle={customBtnStyles}
              iconSrc={addFolderPlus}
            />
          )}
        </AppBar>
        <TabContentsWrap>
          <TabPanel value={value} index={0}>
            {enabledAddFolder && (
              <AddFolderModal
                openModal={enabledAddFolder}
                setOpenModal={setEnableAddFolder}
                childrens={secretsFolder}
                handleSaveClick={addSecretsFolderList}
                parentId={safeDetail.path}
                handleCancelClick={() => setEnableAddFolder(false)}
              />
            )}
            <Secrets
              secretsFolder={secretsFolder}
              value={value}
              secretsStatus={response}
              safeDetail={safeDetail}
              userHavePermission={userHavePermission}
              setEnableAddFolder={setEnableAddFolder}
              getSecretDetails={getSecretDetails}
            />
          </TabPanel>

          <TabPanel value={value} index={1}>
            <Permissions
              safeDetail={safeDetail}
              refresh={refresh}
              permissionResponseType={permissionResponseType}
              safePermissionData={safePermissionData}
              fetchPermission={fetchPermission}
              userDetails={userDetails}
            />
          </TabPanel>
          {toastResponse === -1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              severity="error"
              icon="error"
              message={toastMessage || 'Something went wrong!'}
            />
          )}
          {toastResponse === 1 && (
            <SnackbarComponent
              open
              onClose={() => onToastClose()}
              message={toastMessage || 'Folder added successfully'}
            />
          )}
        </TabContentsWrap>
      </div>
    </ComponentError>
  );
};
SelectionTabs.propTypes = {
  safeDetail: PropTypes.objectOf(PropTypes.any),
  refresh: PropTypes.func.isRequired,
};
SelectionTabs.defaultProps = {
  safeDetail: {},
};

export default SelectionTabs;
