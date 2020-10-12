/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect, useCallback } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import User from './components/User';
import Groups from './components/Groups';
import apiService from '../../apiService';
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
  transform: translate(-50%, -50%);
  position: absolute;
  left: 50%;
  top: 50%;
`;

const Permissions = (props) => {
  const { safeDetail, refresh } = props;
  const [value, setValue] = useState(0);
  const [newPermission, setNewUser] = useState(false);
  const [newGroup, setNewGroup] = useState(false);
  const [newAwsApplication, setNewAwsApplication] = useState(false);
  const [newAppRole, setNewAppRole] = useState(false);
  const [count, setCount] = useState(0);
  const [safeData, setSafeData] = useState({ response: {}, error: '' });
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
    if (safeData?.response && Object.keys(safeData.response).length !== 0) {
      setCount(0);
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
          <PermissionTabsWrapper>
            {' '}
            <TabPanel value={value} index={0}>
              <User
                safeDetail={safeDetail}
                newPermission={newPermission}
                onNewPermissionChange={() => setNewUser(false)}
                fetchPermission={() => fetchPermission()}
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
  refresh: PropTypes.func.isRequired,
};

Permissions.defaultProps = {
  safeDetail: {},
};

export default Permissions;
