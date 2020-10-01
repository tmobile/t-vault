/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import Tab from '@material-ui/core/Tab';
import PropTypes from 'prop-types';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NamedButton from '../../../../../components/NamedButton';
import permissionPlusIcon from '../../../../../assets/permission-plus.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import User from './components/User';
import Groups from './components/Groups';
import apiService from '../../apiService';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import AppRoles from './components/AppRoles';
import SnackbarComponent from '../../../../../components/Snackbar';
import AwsApplications from './components/AwsApplications';

const { belowLarge } = mediaBreakpoints;

const TabPanelWrapper = styled.div`
  height: 90%;
`;

function TabPanel(props) {
  const { children, value, index } = props;
  return (
    <TabPanelWrapper
      role="tabpanel"
      hidden={value !== index}
      id={`scrollable-prevent-tabpanel-${index}`}
      aria-labelledby={`scrollable-prevent-tab-${index}`}
    >
      {children}
    </TabPanelWrapper>
  );
}

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
    id: `scrollable-prevent-tab-${index}`,
    'aria-controls': `scrollable-prevent-tabpanel-${index}`,
  };
}

const TabWrapper = styled.div`
  height: calc(100% - 3.8rem);
  display: flex;
  flex-direction: column;
  position: relative;
  .MuiAppBar-colorPrimary {
    background-color: inherit;
  }
  .MuiPaper-elevation4 {
    box-shadow: none;
  }
  .MuiTabs-root {
    min-height: unset;
  }
  .MuiTab-textColorInherit.Mui-selected {
    background-color: ${(props) => props.theme.palette.secondary.main};
    color: ${(props) => props.theme.palette.secondary.contrastText};
  }
  .MuiTab-textColorInherit {
    color: #e8e8e8;
    background-color: #20232e;
    min-width: auto;
    padding: 8.5px 20px;
    margin-right: 0.5rem;
    font-size: 1.4rem;
    min-height: 3.65rem;
    ${belowLarge} {
      margin-right: 1.2rem;
    }
  }
  .PrivateTabIndicator-colorSecondary-15 {
    background-color: inherit;
  }
`;

const CountPlusWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 2rem;
  height: 3.8rem;
`;
const CountSpan = styled.div`
  color: #5e627c;
  font-size: 1.3rem;
`;

const customStyles = css`
  display: flex;
  ${belowLarge} {
    display: none;
  }
`;

const customMobileStyles = css`
  display: none;
  ${belowLarge} {
    display: flex;
  }
`;

const PermissionTabsWrapper = styled('div')`
  height: calc(100% - 3.7rem);
  overflow: auto;
`;

const customStyle = css`
  height: 100%;
  transform: translate(-50%, -50%);
  position: absolute;
  left: 50%;
  top: 50%;
`;

const useStyles = makeStyles(() => ({
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '3.7rem',
  },
}));

const Permissions = (props) => {
  const { safeDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [newPermission, setNewPermission] = useState(false);
  const [newGroup, setNewGroup] = useState(false);
  const [newAwsApplication, setNewAwsApplication] = useState(false);
  const [newAppRole, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
  const [safeData, setSafeData] = useState({ response: {}, error: '' });
  const [responseType, setResponseType] = useState(0);
  const [selectedTab, setSelectedTab] = useState('Permissions');
  const [toastResponse, setToastResponse] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  const handleChange = (event, newValue) => {
    setValue(newValue);
    setCount(0);
    if (newValue === 0) {
      setSelectedTab('Permission');
    } else if (newValue === 1) {
      setSelectedTab('Group');
    } else if (newValue === 2) {
      setSelectedTab('AWS Application');
    } else {
      setSelectedTab('App Role');
    }
  };

  useEffect(() => {
    if (safeData?.response && Object.keys(safeData?.response).length !== 0) {
      if (value === 0) {
        if (safeData.response.users) {
          setCount(Object.keys(safeData.response.users).length);
        }
      } else if (value === 1) {
        if (safeData.response.groups) {
          setCount(Object.keys(safeData.response.groups).length);
        }
      } else if (value === 2) {
        if (safeData.response['aws-roles']) {
          setCount(Object.keys(safeData.response['aws-roles']).length);
        }
      } else if (value === 3) {
        if (safeData.response['app-roles']) {
          setCount(Object.keys(safeData.response['app-roles']).length);
        }
      }
    }
  }, [value, safeData]);

  const fetchPermission = useCallback(() => {
    setResponseType(0);
    setCount(0);
    setSafeData({});
    apiService
      .getSafeDetails(`${safeDetail.path}`)
      .then((res) => {
        let obj = {};
        setResponseType(1);
        if (res && res.data?.data) {
          obj = res.data.data;
          setSafeData({ response: obj, error: '' });
          setCount(Object.keys(res.data.data.users).length);
        }
      })
      .catch((err) => {
        setResponseType(-1);
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          setSafeData({ response: {}, error: err.response.data.errors[0] });
        }
      });
  }, [safeDetail]);

  useEffect(() => {
    if (safeDetail?.manage) {
      fetchPermission();
    }
  }, [safeDetail, fetchPermission]);

  const onAddLabelBtnClicked = () => {
    if (value === 0) {
      setNewPermission(true);
    } else if (value === 1) {
      setNewGroup(true);
    } else if (value === 2) {
      setNewAwsApplication(true);
    } else {
      setNewAppRole(true);
    }
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setToastResponse(null);
  };
  const updateToastMessage = (response, message) => {
    setToastResponse(response);
    setToastMessage(message);
  };

  return (
    <ComponentError>
      <>
        <CountPlusWrapper>
          <CountSpan color="#5e627c">{`${count} ${selectedTab}s`}</CountSpan>
          <NamedButton
            customStyle={customMobileStyles}
            label={`Add ${selectedTab}`}
            iconSrc={permissionPlusIcon}
            onClick={() => onAddLabelBtnClicked()}
          />
        </CountPlusWrapper>
        <TabWrapper>
          <AppBar position="static" className={classes.appBar}>
            <Tabs
              value={value}
              onChange={handleChange}
              variant="scrollable"
              scrollButtons="off"
              aria-label="scrollable prevent tabs example"
            >
              <Tab label="Users" {...a11yProps(0)} />
              <Tab label="Groups" {...a11yProps(1)} />
              <Tab label="AWS Applications" {...a11yProps(2)} />
              <Tab label="App Roles" {...a11yProps(3)} />
            </Tabs>
            <NamedButton
              customStyle={customStyles}
              label={`Add ${selectedTab}`}
              iconSrc={permissionPlusIcon}
              onClick={() => onAddLabelBtnClicked()}
            />
          </AppBar>
          {responseType === 0 && <LoaderSpinner customStyle={customStyle} />}
          <PermissionTabsWrapper>
            {' '}
            <TabPanel value={value} index={0}>
              <User
                safeDetail={safeDetail}
                newPermission={newPermission}
                onNewPermissionChange={() => setNewPermission(false)}
                fetchPermission={() => fetchPermission()}
                safeData={safeData}
                updateToastMessage={(response, message) =>
                  updateToastMessage(response, message)
                }
              />
            </TabPanel>
            <TabPanel value={value} index={1}>
              <Groups
                safeDetail={safeDetail}
                safeData={safeData}
                fetchPermission={() => fetchPermission()}
                newGroup={newGroup}
                onNewGroupChange={() => setNewGroup(false)}
                updateToastMessage={(response, message) =>
                  updateToastMessage(response, message)
                }
              />
            </TabPanel>
            <TabPanel value={value} index={2}>
              <AwsApplications
                safeDetail={safeDetail}
                safeData={safeData}
                fetchPermission={() => fetchPermission()}
                newAwsApplication={newAwsApplication}
                onNewAwsChange={() => setNewAwsApplication(false)}
                updateToastMessage={(response, message) =>
                  updateToastMessage(response, message)
                }
              />
            </TabPanel>
            <TabPanel value={value} index={3}>
              <AppRoles
                safeDetail={safeDetail}
                safeData={safeData}
                fetchPermission={() => fetchPermission()}
                newAppRole={newAppRole}
                onNewAppRoleChange={() => setNewAppRole(false)}
                updateToastMessage={(response, message) =>
                  updateToastMessage(response, message)
                }
              />
            </TabPanel>
          </PermissionTabsWrapper>
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
              message={toastMessage || 'Successful'}
            />
          )}
        </TabWrapper>
      </>
    </ComponentError>
  );
};

Permissions.propTypes = {
  safeDetail: PropTypes.objectOf(PropTypes.any),
};

Permissions.defaultProps = {
  safeDetail: {},
};

export default Permissions;
