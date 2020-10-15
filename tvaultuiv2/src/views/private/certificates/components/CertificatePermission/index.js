/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import SnackbarComponent from '../../../../../components/Snackbar';
import PermissionsTabs from '../../../../../components/PermissionTabs';
import TabPanel from '../../../../../components/TabPanel';
import { TabWrapper } from '../../../../../styles/GlobalStyles';
import Users from './components/users';
import Groups from './components/groups';

const PermissionTabsWrapper = styled('div')`
  height: calc(100% - 3.7rem);
  overflow: auto;
`;

const CertificatePermission = (props) => {
  const { certificateMetaData, responseStatus, fetchDetail, username } = props;
  const [value, setValue] = useState(0);
  const [newPermission, setNewUser] = useState(false);
  const [newGroup, setNewGroup] = useState(false);
  const [count, setCount] = useState(0);
  const [toastResponse, setToastResponse] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  const initialObject = {
    0: { label: 'users', addBtnCallback: () => setNewUser(true) },
    1: { label: 'groups', addBtnCallback: () => setNewGroup(true) },
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
      }
    }
  }, [value, certificateMetaData]);

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
};

export default CertificatePermission;
