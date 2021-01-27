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
import AddGroup from '../../../../../../../components/AddGroup';
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import Error from '../../../../../../../components/Error';
import { checkAccess } from '../../../../../../../services/helper-function';
import Strings from '../../../../../../../resources';
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

const Groups = (props) => {
  const {
    accountDetail,
    accountMetaData,
    refresh,
    onNewGroupChange,
    newGroup,
    permissionResponse,
    updateToastMessage,
  } = props;

  const [editGroup, setEditGroup] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });

  const isMobileScreen = useMediaQuery(small);

  // on iam svc account meta data is available.
  useEffect(() => {
    setResponse({ status: permissionResponse });
  }, [permissionResponse]);

  // When add group button is clicked.
  useEffect(() => {
    if (newGroup) {
      setResponse({ status: 'add' });
    }
  }, [newGroup]);

  /**
   * @function onDeleteClick
   * @description function to delete the group from the iam svc account group list.
   * @param {username} string groupname of the group.
   * @param {access} string permission of the group.
   */
  const onDeleteClick = (groupname, access) => {
    setResponse({ status: 'loading' });
    const payload = {
      access: checkAccess(access, 'iamsvcaccount'),
      groupname,
      iamSvcAccName: accountDetail.name,
      awsAccountId: `${accountDetail.iamAccountId}`,
    };
    apiService
      .deleteGroupPermission(payload)
      .then(async (res) => {
        if (res?.data?.messages && res.data?.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
          setResponse({ status: '' });
          await refresh();
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
   * @description function to save the group to the iam svc account groups list.
   * @param {data} object payload to call api.
   */
  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    return apiService
      .addGroupPermission(data)
      .then(async (res) => {
        if (res && res.data?.messages) {
          updateToastMessage(1, res.data?.messages[0]);
          setResponse({ status: '' });
          await refresh();
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
   * @param {username} string group name.
   * @param {access} string permission given to the group.
   */
  const onSubmit = async (groupname, access) => {
    const value = {
      access: checkAccess(access, 'iamsvcaccount'),
      groupname: groupname.toLowerCase().replace(/ /g, '-'),
      awsAccountId: `${accountDetail.iamAccountId}`,
      iamSvcAccName: accountDetail.name,
    };
    await onSaveClicked(value);
    onNewGroupChange();
  };

  /**
   * @function onEditSaveClicked
   * @description function to edit the existing group.
   * @param {groupname} string group name to edit.
   * @param {access} string permission given to the group.
   */
  const onEditSaveClicked = (groupname, access) => {
    setResponse({ status: 'loading' });
    const payload = {
      access: checkAccess(access, 'iamsvcaccount'),
      awsAccountId: accountDetail.iamAccountId,
      groupname,
      iamSvcAccName: accountDetail.name,
    };
    apiService
      .deleteGroupPermission(payload)
      .then(async (res) => {
        if (res) {
          setResponse({ status: 'loading' });
          await onSubmit(groupname, access);
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
   * @function onCancelClicked
   * @description function when cancel of add group and edit group is called.
   */
  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewGroupChange();
  };

  /**
   * @function onEditClick
   * @description function to edit the existing group.
   * @param {key} key group name of  the permission.
   * @param {value} value permission given to the group.
   */
  const onEditClick = (key, value) => {
    if (value === 'write') {
      setEditAccess('rotate');
    } else {
      setEditAccess(value);
    }
    setEditGroup(key);
    setResponse({ status: 'edit' });
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'add' && (
          <AddGroup
            groups={accountMetaData?.response?.groups}
            handleSaveClick={(group, access) => onSubmit(group, access)}
            handleCancelClick={onCancelClicked}
            isIamAzureSvcAccount
          />
        )}

        {response.status === 'edit' && (
          <AddGroup
            handleSaveClick={(group, access) =>
              onEditSaveClicked(group, access)
            }
            handleCancelClick={onCancelClicked}
            groupname={editGroup}
            access={editAccess}
            isIamAzureSvcAccount
          />
        )}
        {accountMetaData &&
          accountMetaData.response &&
          response.status === 'success' && (
            <>
              {accountMetaData.response.groups &&
                Object.keys(accountMetaData.response?.groups).length > 0 && (
                  <PermissionsList
                    list={accountMetaData.response.groups}
                    onEditClick={(key, value) => onEditClick(key, value)}
                    onDeleteClick={(key, value) => onDeleteClick(key, value)}
                    isIamAzureSvcAccount
                  />
                )}
              {(!accountMetaData.response.groups ||
                Object.keys(accountMetaData.response?.groups).length === 0) && (
                <NoDataWrapper>
                  <NoData
                    imageSrc={noPermissionsIcon}
                    description={Strings.Resources.noGroupsPermissionFound}
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

Groups.propTypes = {
  accountDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  accountMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  refresh: PropTypes.func.isRequired,
  newGroup: PropTypes.bool.isRequired,
  onNewGroupChange: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
  permissionResponse: PropTypes.string.isRequired,
};
export default Groups;
