/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import SnackbarComponent from '../../../../../components/Snackbar';
import PermissionsTabs from '../../../../../components/PermissionTabs';
import TabPanel from '../../../../../components/TabPanel';
import { TabWrapper } from '../../../../../styles/GlobalStyles';
import Users from './component/Users';
import Groups from './component/Groups';
import AppRoles from './component/AppRoles';
import AwsApplications from './component/AwsApplications';

const PermissionTabsWrapper = styled('div')`
  height: calc(100% - 3.7rem);
  overflow: auto;
`;

const AzurePermission = (props) => {
  const {
    azureMetaData,
    userDetails,
    refresh,
    permissionResponse,
    azureDetail,
  } = props;
  const [value, setValue] = useState(0);
  const [newPermission, setNewUser] = useState(false);
  const [newGroup, setNewGroup] = useState(false);
  const [newAwsApplication, setNewAwsApplication] = useState(false);
  const [newAppRole, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
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
    if (azureMetaData && Object.keys(azureMetaData).length !== 0) {
      setCount(0);
      if (value === 0) {
        if (userDetails) {
          setCount(userDetails.length);
        }
      } else if (value === 1) {
        if (azureMetaData.groups) {
          setCount(Object.keys(azureMetaData.groups).length);
        }
      } else if (value === 2) {
        if (azureMetaData['aws-roles']) {
          setCount(Object.keys(azureMetaData['aws-roles']).length);
        }
      } else if (value === 3) {
        if (azureMetaData['app-roles']) {
          setCount(Object.keys(azureMetaData['app-roles']).length);
        }
      }
    }
  }, [value, azureMetaData, userDetails]);

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
  }, [azureDetail]);

  return (
    <ComponentError>
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
                newPermission={newPermission}
                onNewPermissionChange={() => setNewUser(false)}
                azureMetaData={azureMetaData}
                refresh={refresh}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
                userDetails={userDetails}
                responseStatus={permissionResponse}
              />
            </TabPanel>
            <TabPanel value={value} index={1}>
              <Groups
                azureMetaData={azureMetaData}
                refresh={refresh}
                newGroup={newGroup}
                onNewGroupChange={() => setNewGroup(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
                responseStatus={permissionResponse}
              />
            </TabPanel>
            <TabPanel value={value} index={2}>
              <AwsApplications
                azureMetaData={azureMetaData}
                refresh={refresh}
                newAwsApplication={newAwsApplication}
                onNewAwsChange={() => setNewAwsApplication(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
                responseStatus={permissionResponse}
              />
            </TabPanel>
            <TabPanel value={value} index={3}>
              <AppRoles
                azureMetaData={azureMetaData}
                refresh={refresh}
                newAppRole={newAppRole}
                onNewAppRoleChange={() => setNewAppRole(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
                responseStatus={permissionResponse}
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

AzurePermission.propTypes = {
  azureMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  azureDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  userDetails: PropTypes.arrayOf(PropTypes.any),
  refresh: PropTypes.func.isRequired,
  permissionResponse: PropTypes.objectOf(PropTypes.any).isRequired,
};

AzurePermission.defaultProps = {
  userDetails: [],
};

export default AzurePermission;
