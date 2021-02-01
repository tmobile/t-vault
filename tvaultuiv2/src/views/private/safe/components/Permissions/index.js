/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import User from './components/User';
import Groups from './components/Groups';
import LoaderSpinner from '../../../../../components/Loaders/LoaderSpinner';
import AppRoles from './components/AppRoles';
import SnackbarComponent from '../../../../../components/Snackbar';
import AwsApplications from './components/AwsApplications';
import PermissionsTabs from '../../../../../components/PermissionTabs';
import TabPanel from '../../../../../components/TabPanel';
import { TabWrapper } from '../../../../../styles/GlobalStyles';

const PermissionTabsWrapper = styled('div')`
  height: calc(100% - 3.7rem);
  overflow: auto;
`;

const customStyle = css`
  height: 100%;
`;

const Permissions = (props) => {
  const {
    safeDetail,
    refresh,
    fetchPermission,
    safePermissionData,
    permissionResponseType,
    userDetails,
  } = props;
  const [value, setValue] = useState(0);
  const [newPermission, setNewUser] = useState(false);
  const [newGroup, setNewGroup] = useState(false);
  const [newAwsApplication, setNewAwsApplication] = useState(false);
  const [newAppRole, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
  const [safeData, setSafeData] = useState({});
  const [responseType, setResponseType] = useState(0);
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
    setValue(0);
    setNewUser(false);
    setNewGroup(false);
    setNewAwsApplication(false);
    setNewAppRole(false);
  }, [safeDetail]);

  useEffect(() => {
    if (safeData?.response && Object.keys(safeData.response).length !== 0) {
      setCount(0);
      if (value === 0) {
        if (safeData.response.users) {
          let usersCount = 0;
          Object.entries(safeData.response.users).map(([, data]) => {
            if (data !== 'sudo') {
              usersCount += 1;
            }
            return usersCount;
          });
          setCount(usersCount);
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
  }, [value, safeData, userDetails]);

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
  const updateToastMessage = (response, message) => {
    setToastResponse(response);
    setToastMessage(message);
  };

  const onTabChange = (newValue) => {
    setValue(newValue);
  };

  useEffect(() => {
    if (Object.keys(safePermissionData).length > 0) {
      setSafeData({ ...safePermissionData });
    }
  }, [safePermissionData]);

  useEffect(() => {
    setResponseType(permissionResponseType);
  }, [permissionResponseType]);

  return (
    <ComponentError>
      <>
        <TabWrapper>
          <PermissionsTabs
            count={count}
            value={value}
            onTabChange={onTabChange}
            onAddLabelBtnClicked={onAddLabelBtnClicked}
          />
          {responseType === 0 && <LoaderSpinner customStyle={customStyle} />}
          {responseType === 1 && (
            <PermissionTabsWrapper>
              <TabPanel value={value} index={0}>
                <User
                  safeDetail={safeDetail}
                  userDetails={userDetails}
                  newPermission={newPermission}
                  onNewPermissionChange={() => setNewUser(false)}
                  safeData={safeData}
                  refresh={refresh}
                  updateToastMessage={(response, message) =>
                    updateToastMessage(response, message)
                  }
                />
              </TabPanel>
              <TabPanel value={value} index={1}>
                <Groups
                  safeDetail={safeDetail}
                  safeData={safeData}
                  newGroup={newGroup}
                  refresh={refresh}
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
                  fetchPermission={fetchPermission}
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
                  fetchPermission={fetchPermission}
                  newAppRole={newAppRole}
                  onNewAppRoleChange={() => setNewAppRole(false)}
                  updateToastMessage={(response, message) =>
                    updateToastMessage(response, message)
                  }
                />
              </TabPanel>
            </PermissionTabsWrapper>
          )}
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
  refresh: PropTypes.func.isRequired,
  fetchPermission: PropTypes.func.isRequired,
  safePermissionData: PropTypes.objectOf(PropTypes.any).isRequired,
  permissionResponseType: PropTypes.number,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
};

Permissions.defaultProps = {
  safeDetail: {},
  permissionResponseType: null,
};

export default Permissions;
