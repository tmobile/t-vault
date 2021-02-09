/* eslint-disable react/jsx-indent */
/* eslint-disable react/jsx-curly-newline */
import React, { useState, useEffect } from 'react';
import { css } from 'styled-components';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../../../components/NoData';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import PermissionsList from '../../../../../../../components/PermissionsList';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import mediaBreakpoints from '../../../../../../../breakpoints';
import AddAppRole from '../../../../../../../components/AddAppRole';
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import Error from '../../../../../../../components/Error';
import Strings from '../../../../../../../resources';
import { checkAccess } from '../../../../../../../services/helper-function';
import { NoDataWrapper } from '../../../../../../../styles/GlobalStyles';

const { small, belowLarge } = mediaBreakpoints;

const bgIconStyle = {
  width: '10rem',
  height: '10rem',
};

const customStyle = css`
  height: 100%;
`;

const noDataStyle = css`
  width: 42%;
  ${belowLarge} {
    width: 70%;
  }
  ${small} {
    width: 100%;
  }
`;

const AppRoles = (props) => {
  const {
    accountDetail,
    accountMetaData,
    fetchPermission,
    onNewAppRoleChange,
    newAppRole,
    updateToastMessage,
  } = props;

  const [response, setResponse] = useState({ status: 'loading' });
  const [editRole, setEditRole] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [editClicked, setEditClicked] = useState(false);

  const isMobileScreen = useMediaQuery(small);

  // on svc account meta data is available.
  useEffect(() => {
    if (accountMetaData && Object.keys(accountMetaData).length !== 0) {
      if (Object.keys(accountMetaData?.response).length !== 0) {
        setResponse({ status: 'success' });
      } else if (accountMetaData.error !== '') {
        setResponse({ status: 'error' });
      }
    } else {
      setResponse({ status: '' });
    }
  }, [accountMetaData]);

  // When add app role button is clicked.
  useEffect(() => {
    if (newAppRole) {
      setResponse({ status: 'add' });
    }
  }, [newAppRole]);

  const constructPayload = (role, access) => {
    const data = {
      access: checkAccess(access),
      approlename: role,
      svcAccName: accountDetail.name,
    };
    return data;
  };

  /**
   * @function onDeleteClick
   * @description function to delete the app role from the svc account app role list.
   * @param {role} string app role name.
   * @param {access} string permission of the app role.
   */
  const onDeleteClick = (role, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(role, access);
    apiService
      .deleteAppRolePermission(payload)
      .then(async (res) => {
        if (res && res.data?.messages && res.data?.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
          setResponse({ status: '' });
          await fetchPermission();
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
      });
  };

  /**
   * @function onSaveClicked
   * @description function to save the app rolr to the svc account app role list.
   * @param {data} object payload to call api.
   */
  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    return apiService
      .addAppRolePermission(data)
      .then(async (res) => {
        if (res?.data?.messages && res.data?.messages[0]) {
          updateToastMessage(1, res.data?.messages[0]);
          setResponse({ status: '' });
          await fetchPermission();
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onSubmit
   * @description function structure the payload when save/edit is clicked and call save api.
   * @param {role} string role name.
   * @param {access} string permission given to the app role.
   */
  const onSubmit = async (role, access) => {
    const payload = constructPayload(role, access);
    await onSaveClicked(payload);
    onNewAppRoleChange();
  };

  /**
   * @function onEditSaveClicked
   * @description function to edit the existing app role.
   * @param {role} string app role name to edit.
   * @param {access} string permission given to the app role.
   */
  const onEditSaveClicked = (role, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(role, access);
    apiService
      .deleteAppRolePermission(payload)
      .then(async (res) => {
        if (res) {
          setResponse({ status: 'loading' });
          await onSubmit(role, access);
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onEditClick
   * @description function to edit the existing app role.
   * @param {key} key role name of  the permission.
   * @param {value} value permission given to the app role.
   */
  const onEditClick = (key, value) => {
    setEditClicked(true);
    if (value === 'write') {
      setEditAccess('reset');
    } else {
      setEditAccess(value);
    }
    setEditRole(key);
    setResponse({ status: 'edit' });
  };

  /**
   * @function onCancelClicked
   * @description function when cancel of add app role and edit app role is called.
   */
  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewAppRoleChange();
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'add' && (
          <AddAppRole
            roles={accountMetaData?.response['app-roles']}
            handleSaveClick={(role, access) => onSubmit(role, access)}
            handleCancelClick={() => onCancelClicked()}
            isSvcAccount
          />
        )}
        {response.status === 'edit' && (
          <AddAppRole
            handleSaveClick={(role, access) => onEditSaveClicked(role, access)}
            handleCancelClick={() => onCancelClicked()}
            access={editAccess}
            editClicked={editClicked}
            role={editRole}
            isSvcAccount
          />
        )}

        {accountMetaData &&
          accountMetaData.response &&
          response.status === 'success' && (
            <>
              {accountMetaData.response['app-roles'] &&
                Object.keys(accountMetaData.response['app-roles']).length >
                  0 && (
                  <PermissionsList
                    list={accountMetaData.response['app-roles']}
                    onEditClick={(key, value) => onEditClick(key, value)}
                    onDeleteClick={(key, value) => onDeleteClick(key, value)}
                    isSvcAccount
                  />
                )}
              {(!accountMetaData.response['app-roles'] ||
                Object.keys(accountMetaData.response['app-roles']).length ===
                  0) && (
                <NoDataWrapper>
                  <NoData
                    imageSrc={noPermissionsIcon}
                    description={Strings.Resources.noAppRolePermissionFound}
                    actionButton={
                      // eslint-disable-next-line react/jsx-wrap-multilines
                      <ButtonComponent
                        label="add"
                        icon="add"
                        color="secondary"
                        onClick={() => setResponse({ status: 'add' })}
                        width={isMobileScreen ? '100%' : '9.4rem'}
                      />
                    }
                    bgIconStyle={bgIconStyle}
                    customStyle={noDataStyle}
                  />
                </NoDataWrapper>
              )}
            </>
          )}
        {response.status === 'error' && (
          <Error
            description={accountMetaData.error || 'Something went wrong!'}
          />
        )}
      </>
    </ComponentError>
  );
};

AppRoles.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  accountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  fetchPermission: PropTypes.func.isRequired,
  newAppRole: PropTypes.bool.isRequired,
  onNewAppRoleChange: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
};
export default AppRoles;
