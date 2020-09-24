/* eslint-disable react/jsx-curly-newline */
/* eslint-disable no-console */
import React, { useState, useCallback, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import ComponentError from '../../../../../../../errorBoundaries/ComponentError/component-error';
import NoData from '../../../../../../../components/NoData';
import ButtonComponent from '../../../../../../../components/FormFields/ActionButton';
import {
  TitleTwo,
  TitleFour,
  BackgroundColor,
} from '../../../../../../../styles/GlobalStyles';
import {
  IconDeleteActive,
  IconEdit,
} from '../../../../../../../assets/SvgIcons';
import noPermissionsIcon from '../../../../../../../assets/no-permissions.svg';
import userIcon from '../../../../../../../assets/permission-user.png';
import mediaBreakpoints from '../../../../../../../breakpoints';
import PopperElement from '../../../Popper';
import AddUser from '../../../AddUser';
import apiService from '../../../../apiService';
import SnackbarComponent from '../../../../../../../components/Snackbar';
import LoaderSpinner from '../../../../../../../components/LoaderSpinner';
import Error from '../../../../../../../components/Error';

const { small } = mediaBreakpoints;

const NoDataWrapper = styled.section`
  display: flex;
  justify-content: center;
  margin-top: 2.5rem;
  transform: translate(-50%, -50%);
  position: absolute;
  left: 50%;
  top: 50%;
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
  width: '16rem',
  height: '16rem',
};
const UserList = styled.div`
  margin-top: 2rem;
  > div:not(:last-child) {
    border-bottom: 1px solid #323649;
  }
`;
const EachUserWrap = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const IconDetailsWrap = styled.div`
  display: flex;
`;

const Icon = styled.img`
  width: 5rem;
  height: 5rem;
  margin-right: 1.6rem;
  margin-left: 2rem;
`;

const Details = styled.div``;

const styles = css`
  margin-bottom: 0.5rem;
`;
const permissionStyles = css`
  opacity: 0.7;
`;

const FolderIconWrap = styled('div')`
  margin: 0 1rem;
  display: flex;
  align-items: center;
  cursor: pointer;
  .MuiSvgIcon-root {
    width: 3rem;
    height: 3rem;
    :hover {
      background: #151820;
      border-radius: 50%;
    }
  }
`;
const PopperItem = styled.div`
  padding: 0.5rem;
  display: flex;
  align-items: center;
  flex-direction: row-reverse;
  cursor: pointer;
  span {
    margin-right: 0.75rem;
  }
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
`;

const customStyle = css`
  margin-top: 10rem;
`;

const User = (props) => {
  const {
    safeDetail,
    newPermission,
    onNewPermissionChange,
    getPermissionCount,
  } = props;

  const [editUser, setEditUser] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [toastMessage, setToastMessage] = useState('');
  const [responseType, setResponseType] = useState(0);
  const [users, setUsers] = useState({});
  const [errorMessage, setErrorMessage] = useState('');
  const [response, setResponse] = useState({ status: 'loading' });

  const isMobileScreen = useMediaQuery(small);

  useEffect(() => {
    if (newPermission) {
      setResponse({ status: 'add' });
    }
  }, [newPermission]);

  useEffect(() => {
    getPermissionCount(Object.keys(users).length);
  }, [users, getPermissionCount]);

  const fetchPermission = useCallback(() => {
    setUsers({});
    setResponse({ status: 'loading' });
    apiService
      .getSafePermission(`${safeDetail.path}`)
      .then((res) => {
        setResponseType(null);
        if (res && res.data?.data?.users) {
          setUsers(res.data.data.users);
          setResponse({ status: 'display' });
        }
      })
      .catch((err) => {
        setResponse({ status: 'error' });
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          setErrorMessage(err.response?.data?.errors[0]);
        }
      });
  }, [safeDetail]);

  useEffect(() => {
    if (safeDetail?.manage) {
      fetchPermission();
    }
  }, [safeDetail, fetchPermission]);

  const onSaveClicked = (data) => {
    setResponse({ status: 'loading' });
    apiService
      .addUserPermission(data)
      .then((res) => {
        if (res && res.data?.messages) {
          setResponseType(1);
          setToastMessage(res.data?.messages[0]);
          fetchPermission();
        }
      })
      .catch((err) => {
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponse({ status: 'display' });
        setResponseType(-1);
      });
  };

  const onDeleteClick = (username) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: safeDetail.path,
      username,
    };
    apiService
      .deleteUserPermission(payload)
      .then((res) => {
        if (res && res.data?.Message) {
          setToastMessage(res.data.Message);
          setResponseType(1);
          fetchPermission();
        }
      })
      .catch((err) => {
        setResponse({ status: 'display' });
        if (err.response?.data?.errors && err.response.data.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
      });
  };

  const onSubmit = (user, access) => {
    const value = {
      access,
      path: `${safeDetail.path}`,
      username: user.toLowerCase(),
    };
    onSaveClicked(value);
    onNewPermissionChange();
  };

  const onEditSaveClicked = (username, access) => {
    setResponse({ status: 'loading' });
    const payload = {
      path: `${safeDetail.path}dbbdb`,
      username,
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
          setToastMessage(err.response.data.errors[0]);
        }
        setResponse({ status: 'display' });
        setResponseType(-1);
      });
  };

  const onCancelClicked = () => {
    setResponse({ status: 'display' });
    onNewPermissionChange();
  };

  const onEditClick = (key, value) => {
    setEditAccess(value);
    setEditUser(key);
    setResponse({ status: 'edit' });
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  return (
    <ComponentError>
      {response.status === 'loading' && (
        <LoaderSpinner customStyle={customStyle} />
      )}
      <>
        {response.status === 'add' && (
          <AddUser
            handleSaveClick={(user, access) => onSubmit(user, access)}
            handleCancelClick={onCancelClicked}
          />
        )}
        {response.status === 'edit' && (
          <AddUser
            handleSaveClick={(user, access) => onEditSaveClicked(user, access)}
            handleCancelClick={onCancelClicked}
            username={editUser}
            access={editAccess}
          />
        )}
        {users &&
          Object.keys(users).length > 0 &&
          response.status === 'display' && (
            <UserList>
              {Object.entries(users).map(([key, value]) => (
                <EachUserWrap key={key}>
                  <IconDetailsWrap>
                    <Icon src={userIcon} alt="user" />
                    <Details>
                      <TitleTwo extraCss={styles}>{key}</TitleTwo>
                      <TitleFour extraCss={permissionStyles}>
                        2 days ago
                        {' - '}
                        {value}
                      </TitleFour>
                    </Details>
                  </IconDetailsWrap>
                  <FolderIconWrap>
                    <PopperElement
                      anchorOrigin={{
                        vertical: 'bottom',
                        horizontal: 'right',
                      }}
                      transformOrigin={{
                        vertical: 'top',
                        horizontal: 'right',
                      }}
                    >
                      <PopperItem onClick={() => onEditClick(key, value)}>
                        <IconEdit />
                        <span>Edit</span>
                      </PopperItem>
                      <PopperItem onClick={() => onDeleteClick(key)}>
                        <IconDeleteActive />
                        <span> Delete</span>
                      </PopperItem>
                    </PopperElement>
                  </FolderIconWrap>
                </EachUserWrap>
              ))}
            </UserList>
          )}
        {users &&
          Object.keys(users).length === 0 &&
          response.status === 'display' && (
            <NoDataWrapper>
              <NoData
                imageSrc={noPermissionsIcon}
                description="Add <strong>Permissions</strong> to allow people, 
                groups or aplication to access this safe"
                actionButton={
                  // eslint-disable-next-line react/jsx-wrap-multilines
                  <ButtonComponent
                    label="add"
                    icon="add"
                    color="secondary"
                    onClick={() => setResponse({ status: 'add' })}
                    width={isMobileScreen ? '100%' : '38%'}
                  />
                }
                bgIconStyle={bgIconStyle}
                width={isMobileScreen ? '100%' : '38%'}
              />
            </NoDataWrapper>
          )}
      </>
      {response.status === 'error' && (
        <Error description={errorMessage || 'Something went wrong'} />
      )}
      {responseType === -1 && (
        <SnackbarComponent
          open
          onClose={() => onToastClose()}
          severity="error"
          icon="error"
          message={toastMessage || 'Something went wrong!'}
        />
      )}
      {responseType === 1 && (
        <SnackbarComponent
          open
          onClose={() => onToastClose()}
          message={toastMessage || 'Successful'}
        />
      )}
    </ComponentError>
  );
};

User.propTypes = {
  safeDetail: PropTypes.objectOf(PropTypes.any).isRequired,
  newPermission: PropTypes.bool.isRequired,
  onNewPermissionChange: PropTypes.func.isRequired,
  getPermissionCount: PropTypes.func.isRequired,
};
export default User;
