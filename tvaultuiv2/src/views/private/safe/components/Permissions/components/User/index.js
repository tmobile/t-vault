import React, { useState } from 'react';
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
import SnackbarComponent from '../../../../../../../components/Snackbar';
import apiService from '../../../../apiService';

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

const User = (props) => {
  const {
    users,
    onSaveClicked,
    addPermission,
    onNoDataAddClicked,
    onCancelClicked,
  } = props;
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [editUser, setEditUser] = useState('');
  const [editAccess, setEditAccess] = useState('');
  const [editPermission, setEditPermission] = useState(false);
  const isMobileScreen = useMediaQuery(small);

  const onSubmit = (user, access) => {
    const value = {
      access,
      path: 'shared/test-safe3',
      username: user.toLowerCase(),
    };
    onSaveClicked(value);
  };

  const onDeleteClick = (username) => {
    const payload = {
      path: 'shared/test-safe3',
      username,
    };
    apiService
      .deleteApiCall('/vault/v2/ss/sdb/user', payload)
      .then((res) => {
        if (res && res.data?.Message) {
          setToastMessage(res.data.Message);
          setResponseType(1);
        }
      })
      .catch((err) => {
        if (err.response && err.response.data?.messages[0]) {
          setToastMessage(err.response.data.messages[0]);
        }
        setResponseType(-1);
      });
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  const onEditClick = (key, value) => {
    setEditAccess(value);
    setEditUser(key);
    setEditPermission(true);
  };

  const onEditCancelClicked = () => {
    setEditPermission(false);
    onCancelClicked();
  };

  return (
    <ComponentError>
      <>
        {addPermission && !editPermission && (
          <AddUser
            handleSaveClick={(user, access) => onSubmit(user, access)}
            handleCancelClick={onCancelClicked}
          />
        )}
        {editPermission && (
          <AddUser
            handleSaveClick={(user, access) => onSubmit(user, access)}
            handleCancelClick={onEditCancelClicked}
            username={editUser}
            access={editAccess}
          />
        )}
        {users &&
        Object.keys(users).length > 0 &&
        !addPermission &&
        !editPermission ? (
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
        ) : (
          !addPermission &&
          !editPermission && (
            <NoDataWrapper>
              <NoData
                imageSrc={noPermissionsIcon}
                description="Add <strong>Permissions</strong> to allow people, groups or aplication to access this safe"
                actionButton={
                  // eslint-disable-next-line react/jsx-wrap-multilines
                  <ButtonComponent
                    label="add"
                    icon="add"
                    color="secondary"
                    onClick={onNoDataAddClicked}
                    width={isMobileScreen ? '100%' : '38%'}
                  />
                }
                bgIconStyle={bgIconStyle}
                width={isMobileScreen ? '100%' : '38%'}
              />
            </NoDataWrapper>
          )
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
            message={toastMessage}
          />
        )}
      </>
    </ComponentError>
  );
};

User.propTypes = {
  users: PropTypes.objectOf(PropTypes.any).isRequired,
  onSaveClicked: PropTypes.func.isRequired,
  addPermission: PropTypes.bool.isRequired,
  onNoDataAddClicked: PropTypes.func.isRequired,
  onCancelClicked: PropTypes.func.isRequired,
};
export default User;
