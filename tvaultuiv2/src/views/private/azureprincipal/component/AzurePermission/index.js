/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import SnackbarComponent from '../../../../../components/Snackbar';
import PermissionsTabs from '../../../../../components/PermissionTabs';
import TabPanel from '../../../../../components/TabPanel';
import { TabWrapper } from '../../../../../styles/GlobalStyles';
// import Users from './components/Users';
// import Groups from './components/Groups';
// import AppRoles from './components/AppRoles';
// import AwsApplications from './components/AwsApplications';

const PermissionTabsWrapper = styled('div')`
  height: calc(100% - 3.7rem);
  overflow: auto;
`;

const AzurePermission = (props) => {
  const { azureMetaData } = props;
  const [value, setValue] = useState(0);
  const [, setNewUser] = useState(false);
  const [, setNewGroup] = useState(false);
  const [, setNewAwsApplication] = useState(false);
  const [, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
  const [toastResponse, setToastResponse] = useState(null);
  const [toastMessage] = useState('');

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
        // if (azureMetaData) {
        //   setCount(userDetails.length - 1);
        // }
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
  }, [value, azureMetaData]);

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

  //   const updateToastMessage = (res, message) => {
  //     setToastResponse(res);
  //     setToastMessage(message);
  //   };

  const onTabChange = (newValue) => {
    setValue(newValue);
  };

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
              users
              {/* <Users
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                responseStatus={responseStatus}
                username={username}
                newPermission={newPermission}
                onNewPermissionChange={() => setNewUser(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
                userDetails={userDetails}
              /> */}
            </TabPanel>
            <TabPanel value={value} index={1}>
              groups
              {/* <Groups
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                responseStatus={responseStatus}
                username={username}
                newGroup={newGroup}
                onNewGroupChange={() => setNewGroup(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
              /> */}
            </TabPanel>
            <TabPanel value={value} index={2}>
              aws
              {/* <AwsApplications
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                responseStatus={responseStatus}
                username={username}
                newAwsApplication={newAwsApplication}
                onNewAwsChange={() => setNewAwsApplication(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
              /> */}
            </TabPanel>
            <TabPanel value={value} index={3}>
              approles
              {/* <AppRoles
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                username={username}
                responseStatus={responseStatus}
                newAppRole={newAppRole}
                onNewAppRoleChange={() => setNewAppRole(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
              /> */}
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
};

export default AzurePermission;
