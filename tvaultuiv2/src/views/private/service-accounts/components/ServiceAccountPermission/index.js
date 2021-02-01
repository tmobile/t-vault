/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import Users from './components/Users';
import Groups from './components/Groups';
import AppRoles from './components/AppRoles';
import AwsApplications from './components/AwsApplications';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import Error from '../../../../../components/Error';
import SnackbarComponent from '../../../../../components/Snackbar';
import PermissionsTabs from '../../../../../components/PermissionTabs';
import TabPanel from '../../../../../components/TabPanel';
import { TabWrapper } from '../../../../../styles/GlobalStyles';

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

const ServiceAccountPermission = (props) => {
  const {
    accountDetail,
    refresh,
    accountMetaData,
    hasSvcAccountAcitve,
    parentStatus,
    fetchPermission,
    userDetails,
  } = props;
  const [value, setValue] = useState(0);
  const [newPermission, setNewUser] = useState(false);
  const [newGroup, setNewGroup] = useState(false);
  const [newAwsApplication, setNewAwsApplication] = useState(false);
  const [newAppRole, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
  const [response, setResponse] = useState({ status: 'loading' });
  const [toastResponse, setToastResponse] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  const initialObject = {
    0: { label: 'users', addBtnCallback: () => setNewUser(true) },
    1: { label: 'groups', addBtnCallback: () => setNewGroup(true) },
    2: {
      label: 'aws applications',
      addBtnCallback: () => setNewAwsApplication(true),
    },
    3: { label: 'app roles', addBtnCallback: () => setNewAppRole(true) },
  };

  useEffect(() => {
    setResponse({ status: parentStatus });
  }, [parentStatus]);

  useEffect(() => {
    if (
      accountMetaData.response &&
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
  }, [value, accountMetaData, userDetails]);

  const onAddLabelBtnClicked = () => {
    Object.keys(initialObject).map((item) => {
      if (item === value.toString()) {
        initialObject[item].addBtnCallback();
      }
      return null;
    });
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

  const onTabChange = (newValue) => {
    setValue(newValue);
  };

  useEffect(() => {
    setValue(0);
    setNewUser(false);
    setNewGroup(false);
    setNewAppRole(false);
    setNewAwsApplication(false);
  }, [accountDetail]);

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
            {hasSvcAccountAcitve ? (
              <>
                <TabWrapper>
                  <PermissionsTabs
                    onAddLabelBtnClicked={onAddLabelBtnClicked}
                    count={count}
                    onTabChange={onTabChange}
                    value={value}
                  />
                  <PermissionTabsWrapper>
                    <TabPanel value={value} index={0}>
                      <Users
                        accountDetail={accountDetail}
                        newPermission={newPermission}
                        onNewPermissionChange={() => setNewUser(false)}
                        accountMetaData={accountMetaData}
                        refresh={refresh}
                        updateToastMessage={(res, message) =>
                          updateToastMessage(res, message)
                        }
                        userDetails={userDetails}
                      />
                    </TabPanel>
                    <TabPanel value={value} index={1}>
                      <Groups
                        accountDetail={accountDetail}
                        newGroup={newGroup}
                        onNewGroupChange={() => setNewGroup(false)}
                        accountMetaData={accountMetaData}
                        updateToastMessage={(res, message) =>
                          updateToastMessage(res, message)
                        }
                        refresh={refresh}
                      />
                    </TabPanel>
                    <TabPanel value={value} index={2}>
                      <AwsApplications
                        accountDetail={accountDetail}
                        newAwsApplication={newAwsApplication}
                        onNewAwsChange={() => setNewAwsApplication(false)}
                        accountMetaData={accountMetaData}
                        updateToastMessage={(res, message) =>
                          updateToastMessage(res, message)
                        }
                        fetchPermission={fetchPermission}
                      />
                    </TabPanel>
                    <TabPanel value={value} index={3}>
                      <AppRoles
                        accountDetail={accountDetail}
                        newAppRole={newAppRole}
                        onNewAppRoleChange={() => setNewAppRole(false)}
                        accountMetaData={accountMetaData}
                        updateToastMessage={(res, message) =>
                          updateToastMessage(res, message)
                        }
                        fetchPermission={fetchPermission}
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
            ) : (
              <NoPermission>Please activate the service account</NoPermission>
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
  hasSvcAccountAcitve: PropTypes.bool,
  parentStatus: PropTypes.string,
  fetchPermission: PropTypes.func,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
};

ServiceAccountPermission.defaultProps = {
  accountDetail: {},
  hasSvcAccountAcitve: false,
  fetchPermission: () => {},
  parentStatus: '',
};

export default ServiceAccountPermission;
