/* eslint-disable react/jsx-indent */
import React, { useState, useEffect } from 'react';
import { css } from 'styled-components';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../../../components/NoData';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import mediaBreakpoints from '../../../../../../../breakpoints';
import AddUser from '../../../../../../../components/AddUser';
import apiService from '../../../../apiService';
import LoaderSpinner from '../../../../../../../components/Loaders/LoaderSpinner';
import CertificatePermissionsList from '../../../PermissionList';
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

const Users = (props) => {
  const {
    certificateMetaData,
    newPermission,
    onNewPermissionChange,
    updateToastMessage,
    responseStatus,
    refresh,
    username,
    userDetails,
  } = props;

  const [editUser, setEditUser] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });
  const isMobileScreen = useMediaQuery(small);

  // on certificate meta data is available.
  useEffect(() => {
    setResponse({ status: responseStatus });
  }, [responseStatus]);

  // When add permission button is clicked.
  useEffect(() => {
    if (newPermission) {
      setResponse({ status: 'add' });
    }
  }, [newPermission]);

  const constructPayload = (userName, access) => {
    const data = {
      access,
      certType: certificateMetaData.certType,
      certificateName: certificateMetaData.certificateName,
      username: userName,
    };
    return data;
  };

  /**
   * @function onDeleteClick
   * @description function to delete the user from the certificate users list.
   * @param {username} string username of the user.
   * @param {access} string permission of the user.
   */
  const onDeleteClick = async (userName, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(userName, access);
    apiService
      .deleteCertificateUser(payload)
      .then(async (res) => {
        if (res && res.data?.messages && res.data.messages[0]) {
          updateToastMessage(1, res.data.messages[0]);
          setResponse({ status: '' });
          await refresh();
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        if (err.response?.data?.errors && err?.response?.data?.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
      });
  };

  /**
   * @function onSaveClicked
   * @description function to save the user to the certificate users list.
   * @param {data} object payload to call api.
   */
  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    apiService
      .addCertificateUser(data)
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
   * @param {username} string user name of the user.
   * @param {access} string permission given to the user.
   */
  const onSubmit = async (userName, access) => {
    const payload = constructPayload(userName, access);
    try {
      await onSaveClicked(payload);
      onNewPermissionChange();
    } catch {
      setResponse({ status: 'success' });
      updateToastMessage(-1, 'Something went wrong');
    }
  };

  /**
   * @function onEditSaveClicked
   * @description function to edit the existing user.
   * @param {username} string user name of the user.
   * @param {access} string permission given to the user.
   */
  const onEditSaveClicked = (userName, access) => {
    setResponse({ status: 'loading' });
    const payload = constructPayload(userName, access);
    apiService
      .deleteCertificateUser(payload)
      .then(async (res) => {
        if (res) {
          setResponse({ status: 'loading' });
          await onSubmit(userName, access);
        }
      })
      .catch((err) => {
        if (err.response?.data?.messages && err.response.data.messages[0]) {
          updateToastMessage(-1, err.response.data.messages[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  /**
   * @function onCancelClicked
   * @description function when cancel of add user and edit user is called.
   */
  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewPermissionChange();
  };

  useEffect(() => {
    onCancelClicked();
    // eslint-disable-next-line
  }, [certificateMetaData]);

  /**
   * @function onEditClick
   * @description function to edit the existing user.
   * @param {key} key user name of the user.
   * @param {value} value permission given to the user.
   */
  const onEditClick = (key, value) => {
    setEditAccess(value);
    setEditUser(key);
    setResponse({ status: 'edit' });
  };

  return (
    <ComponentError>
      <>
        {response.status === 'loading' && (
          <LoaderSpinner customStyle={customStyle} />
        )}
        {response.status === 'add' && (
          <AddUser
            users={certificateMetaData.users}
            handleSaveClick={(user, access) => onSubmit(user, access)}
            handleCancelClick={onCancelClicked}
            refresh={refresh}
            isCertificate
          />
        )}
        {response.status === 'edit' && (
          <AddUser
            handleSaveClick={(user, access) => onEditSaveClicked(user, access)}
            handleCancelClick={onCancelClicked}
            username={editUser}
            access={editAccess}
            refresh={refresh}
            isCertificate
          />
        )}
        {response.status === 'success' &&
          Object.keys(certificateMetaData).length > 0 && (
            <>
              {certificateMetaData.users &&
                userDetails.length > 0 &&
                Object.keys(certificateMetaData.users).length > 1 && (
                  <CertificatePermissionsList
                    list={certificateMetaData.users}
                    username={username}
                    onEditClick={(key, value) => onEditClick(key, value)}
                    onDeleteClick={(key, value) => onDeleteClick(key, value)}
                    userDetails={userDetails}
                  />
                )}
              {(!certificateMetaData.users ||
                userDetails.length === 0 ||
                Object.keys(certificateMetaData.users).length === 1) && (
                <NoDataWrapper>
                  <NoData
                    imageSrc={noPermissionsIcon}
                    description={Strings.Resources.noUsersPermissionFound}
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
      </>
    </ComponentError>
  );
};

Users.propTypes = {
  responseStatus: PropTypes.string.isRequired,
  newPermission: PropTypes.bool.isRequired,
  onNewPermissionChange: PropTypes.func.isRequired,
  certificateMetaData: PropTypes.objectOf(PropTypes.any).isRequired,
  updateToastMessage: PropTypes.func.isRequired,
  refresh: PropTypes.func.isRequired,
  username: PropTypes.string.isRequired,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
};
export default Users;
