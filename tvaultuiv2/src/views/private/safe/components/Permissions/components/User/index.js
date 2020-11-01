/* eslint-disable react/jsx-indent */
import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
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
import PermissionsList from '../../../../../../../components/PermissionsList';
import Error from '../../../../../../../components/Error';

const { small, belowLarge } = mediaBreakpoints;

const NoDataWrapper = styled.section`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  width: 100%;

  p {
    ${small} {
      margin-top: 2rem;
      margin-bottom: 4rem;
      width: 75%;
    }
  }
`;

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

const User = (props) => {
  const {
    safeDetail,
    newPermission,
    onNewPermissionChange,
    safeData,
    fetchPermission,
    updateToastMessage,
    refresh,
  } = props;

  const [editUser, setEditUser] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });
  const isMobileScreen = useMediaQuery(small);

  useEffect(() => {
    if (safeData && Object.keys(safeData).length !== 0) {
      if (Object.keys(safeData?.response).length !== 0) {
        setResponse({ status: 'success' });
      } else if (safeData.error !== '') {
        setResponse({ status: 'error' });
      }
    } else {
      setResponse({ status: '' });
    }
  }, [safeData]);

  useEffect(() => {
    if (newPermission) {
      setResponse({ status: 'add' });
    }
  }, [newPermission]);

  const onDeleteClick = (username) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: safeDetail.path,
      username,
    };
    apiService
      .deleteUserPermission(payload)
      .then(async (res) => {
        if (res && res.data?.Message) {
          updateToastMessage(1, res.data.Message);
          setResponse({ status: '' });
          await fetchPermission();
          refresh();
        }
      })
      .catch((err) => {
        setResponse({ status: 'success' });
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
      });
  };

  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    apiService
      .addUserPermission(data)
      .then(async (res) => {
        if (res && res.data?.messages) {
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

  const onSubmit = async (user, access) => {
    const value = {
      access,
      path: `${safeDetail.path}`,
      username: user.toLowerCase(),
    };
    await onSaveClicked(value);
    refresh();
    onNewPermissionChange();
  };

  const onEditSaveClicked = (username, access) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: `${safeDetail.path}`,
      username,
      access,
    };
    apiService
      .deleteUserPermission(payload)
      .then((res) => {
        if (res) {
          setResponse({ status: 'loading' });
          onSubmit(username, access);
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          updateToastMessage(-1, err.response.data.errors[0]);
        }
        setResponse({ status: 'success' });
      });
  };

  const onCancelClicked = () => {
    setResponse({ status: 'success' });
    onNewPermissionChange();
  };

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
            handleSaveClick={(user, access) => onSubmit(user, access)}
            handleCancelClick={onCancelClicked}
            refresh={refresh}
          />
        )}
        {response.status === 'edit' && (
          <AddUser
            handleSaveClick={(user, access) => onEditSaveClicked(user, access)}
            handleCancelClick={onCancelClicked}
            username={editUser}
            access={editAccess}
            refresh={refresh}
          />
        )}
        {response.status === 'success' && safeData && safeData.response && (
          <>
            {safeData.response?.users &&
              Object.keys(safeData.response?.users).length > 0 && (
                <PermissionsList
                  list={safeData.response.users}
                  onEditClick={(key, value) => onEditClick(key, value)}
                  onDeleteClick={(key) => onDeleteClick(key)}
                />
              )}
            {(safeData.response.users === null ||
              !safeData.response.users ||
              (safeData.response.users &&
                Object.keys(safeData.response.users).length === 0)) && (
              <NoDataWrapper>
                <NoData
                  imageSrc={noPermissionsIcon}
                  description="No <strong>users</strong> are given permission to access this safe,
                    add users to access the safe"
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
          <Error description={safeData.error || 'Something went wrong'} />
        )}
      </>
    </ComponentError>
  );
};

User.propTypes = {
  safeDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  newPermission: PropTypes.bool.isRequired,
  onNewPermissionChange: PropTypes.func.isRequired,
  safeData: PropTypes.objectOf(PropTypes.any).isRequired,
  fetchPermission: PropTypes.func.isRequired,
  updateToastMessage: PropTypes.func.isRequired,
  refresh: PropTypes.func.isRequired,
};
export default User;
