/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState, useEffect } from 'react';
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
import Users from './components/Users';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import Error from '../../../../../components/Error';
import SnackbarComponent from '../../../../../components/Snackbar';

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

const NoPermission = styled.div`
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5a637a;
`;

const useStyles = makeStyles(() => ({
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    height: '3.7rem',
  },
}));

const ServiceAccountPermission = (props) => {
  const {
    accountDetail,
    refresh,
    accountMetaData,
    hasPermission,
    parentStatus,
  } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [newPermission, setNewPermission] = useState(false);
  const [, setNewGroup] = useState(false);
  const [, setNewAwsApplication] = useState(false);
  const [, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
  const [response, setResponse] = useState({ status: 'loading' });
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
    } else if (newValue === 3) {
      setSelectedTab('App Role');
    }
  };

  useEffect(() => {
    setResponse({ status: parentStatus });
  }, [parentStatus]);

  useEffect(() => {
    if (
      accountMetaData?.response &&
      Object.keys(accountMetaData?.response).length !== 0
    ) {
      setCount(0);
      if (value === 0) {
        if (accountMetaData.response.users) {
          setCount(Object.keys(accountMetaData.response.users).length);
        }
      } else if (value === 1) {
        if (accountMetaData.response.groups) {
          setCount(Object.keys(accountMetaData.response.groups).length);
        }
      } else if (value === 2) {
        if (accountMetaData.response['aws-roles']) {
          setCount(Object.keys(accountMetaData.response['aws-roles']).length);
        }
      } else if (value === 3) {
        if (accountMetaData.response['app-roles']) {
          setCount(Object.keys(accountMetaData.response['app-roles']).length);
        }
      }
    }
  }, [value, accountMetaData]);

  const onAddLabelBtnClicked = () => {
    if (value === 0) {
      setNewPermission(true);
    } else if (value === 1) {
      setNewGroup(true);
    } else if (value === 2) {
      setNewAwsApplication(true);
    } else if (value === 3) {
      setNewAppRole(true);
    }
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setToastResponse(null);
  };
  const updateToastMessage = (res, message) => {
    setToastResponse(res);
    setToastMessage(message);
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'error' && (
          <Error description="Something went wrong!" />
        )}
        {response.status === 'success' && (
          <>
            {hasPermission ? (
              <>
                {' '}
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
                  <PermissionTabsWrapper>
                    {' '}
                    <TabPanel value={value} index={0}>
                      <Users
                        accountDetail={accountDetail}
                        newPermission={newPermission}
                        onNewPermissionChange={() => setNewPermission(false)}
                        accountMetaData={accountMetaData}
                        refresh={refresh}
                        updateToastMessage={(res, message) =>
                          updateToastMessage(res, message)
                        }
                      />
                    </TabPanel>
                    <TabPanel value={value} index={1}>
                      groups
                    </TabPanel>
                    <TabPanel value={value} index={2}>
                      AwsApplications
                    </TabPanel>
                    <TabPanel value={value} index={3}>
                      Approle
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
            ) : (
              <NoPermission>Access denied</NoPermission>
            )}
          </>
        )}
      </>
    </ComponentError>
  );
};

ServiceAccountPermission.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any),
  accountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  refresh: PropTypes.func.isRequired,
  hasPermission: PropTypes.bool,
  parentStatus: PropTypes.objectOf(PropTypes.any).isRequired,
};

ServiceAccountPermission.defaultProps = {
  accountDetail: {},
  hasPermission: false,
};

export default ServiceAccountPermission;
