/* eslint-disable react/jsx-curly-newline */
/* eslint-disable no-nested-ternary */
import React from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import userIcon from '../../assets/permission-user.png';
import EditDeletePopper from '../../views/private/safe/components/EditDeletePopper';
import {
  TitleTwo,
  TitleFour,
  BackgroundColor,
} from '../../styles/GlobalStyles';
import { useStateValue } from '../../contexts/globalState';

const UserList = styled.div`
  margin-top: 2rem;
  > div:not(:last-child) {
    border-bottom: 1px solid #323649;
  }
`;

const Wrap = styled.div``;

const EachUserWrap = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${BackgroundColor.listBg};
  padding: 1.2rem 0;
  :hover {
    background-image: ${(props) => props.theme.gradients.list || 'none'};
  }
  ${(props) => props.inActitveStyles}
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

const UserPermissionsList = (props) => {
  const {
    onEditClick,
    list,
    onDeleteClick,
    isSvcAccount,
    isIamAzureSvcAccount,
    userDetails,
  } = props;
  // get logged in user info
  const state = useStateValue();

  const getUsersDisplayName = (userName) => {
    const obj = userDetails.find(
      (item) => item.userName.toLowerCase() === userName.toLowerCase()
    );
    if (obj) {
      return `${obj.displayName} (${obj.userName})`;
    }
    return userName;
  };

  return (
    <UserList>
      {Object.entries(list).map(([key, value]) => (
        <Wrap key={key}>
          {value?.toLowerCase() !== 'sudo' && (
            <EachUserWrap
              inActitveStyles={
                state[0]?.username?.toLowerCase() === key?.toLowerCase() &&
                isIamAzureSvcAccount
                  ? 'pointer-events:none;opacity:0.5'
                  : ''
              }
            >
              <IconDetailsWrap>
                <Icon src={userIcon} alt="user" />
                <Details>
                  <TitleTwo extraCss={styles}>
                    {getUsersDisplayName(key)}
                  </TitleTwo>
                  <TitleFour extraCss={permissionStyles}>
                    {isSvcAccount && value === 'write'
                      ? 'reset'
                      : isIamAzureSvcAccount && value === 'write'
                      ? 'rotate'
                      : value}
                  </TitleFour>
                </Details>
              </IconDetailsWrap>
              <EditDeletePopper
                onEditClicked={() =>
                  onEditClick(getUsersDisplayName(key), value)
                }
                onDeleteClicked={() => onDeleteClick(key, value)}
              />
            </EachUserWrap>
          )}
        </Wrap>
      ))}
    </UserList>
  );
};
UserPermissionsList.propTypes = {
  onDeleteClick: PropTypes.func.isRequired,
  onEditClick: PropTypes.func.isRequired,
  list: PropTypes.objectOf(PropTypes.any).isRequired,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
  isSvcAccount: PropTypes.bool,
  isIamAzureSvcAccount: PropTypes.bool,
};

UserPermissionsList.defaultProps = {
  isSvcAccount: false,
  isIamAzureSvcAccount: false,
};

export default UserPermissionsList;
