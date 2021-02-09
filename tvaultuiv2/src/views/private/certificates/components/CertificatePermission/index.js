/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import SnackbarComponent from '../../../../../components/Snackbar';
import PermissionsTabs from '../../../../../components/PermissionTabs';
import TabPanel from '../../../../../components/TabPanel';
import { TabWrapper } from '../../../../../styles/GlobalStyles';
import Users from './components/Users';
import Groups from './components/Groups';
import AppRoles from './components/AppRoles';
import AwsApplications from './components/AwsApplications';

const PermissionTabsWrapper = styled('div')`
  height: calc(100% - 3.7rem);
  overflow: auto;
`;

const CertificatePermission = (props) => {
  const {
    certificateMetaData,
    responseStatus,
    fetchDetail,
    username,
    userDetails,
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
    if (certificateMetaData && Object.keys(certificateMetaData).length !== 0) {
      setCount(0);
      if (value === 0) {
        if (certificateMetaData.users) {
          setCount(Object.keys(certificateMetaData.users).length - 1);
        }
      } else if (value === 1) {
        if (certificateMetaData.groups) {
          setCount(Object.keys(certificateMetaData.groups).length);
        }
      } else if (value === 2) {
        if (certificateMetaData['aws-roles']) {
          setCount(Object.keys(certificateMetaData['aws-roles']).length);
        }
      } else if (value === 3) {
        if (certificateMetaData['app-roles']) {
          setCount(Object.keys(certificateMetaData['app-roles']).length);
        }
      }
    }
  }, [value, certificateMetaData, userDetails]);

  useEffect(() => {
    setValue(0);
  }, [certificateMetaData]);

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

  return (
    <ComponentError>
      <>
        <TabWrapper>
          <PermissionsTabs
            onAddLabelBtnClicked={onAddLabelBtnClicked}
            count={count}
            onTabChange={onTabChange}
            value={value}
            certificate
          />
          <PermissionTabsWrapper>
            <TabPanel value={value} index={0}>
              <Users
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
              />
            </TabPanel>
            <TabPanel value={value} index={1}>
              <Groups
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                responseStatus={responseStatus}
                username={username}
                newGroup={newGroup}
                onNewGroupChange={() => setNewGroup(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
              />
            </TabPanel>
            <TabPanel value={value} index={2}>
              <AwsApplications
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                responseStatus={responseStatus}
                username={username}
                newAwsApplication={newAwsApplication}
                onNewAwsChange={() => setNewAwsApplication(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
                }
              />
            </TabPanel>
            <TabPanel value={value} index={3}>
              <AppRoles
                certificateMetaData={certificateMetaData}
                refresh={fetchDetail}
                username={username}
                responseStatus={responseStatus}
                newAppRole={newAppRole}
                onNewAppRoleChange={() => setNewAppRole(false)}
                updateToastMessage={(res, message) =>
                  updateToastMessage(res, message)
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

CertificatePermission.propTypes = {
  certificateMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  fetchDetail: PropTypes.func.isRequired,
  responseStatus: PropTypes.string.isRequired,
  username: PropTypes.string.isRequired,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
};

export default CertificatePermission;
