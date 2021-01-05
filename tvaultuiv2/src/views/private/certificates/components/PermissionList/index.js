/* eslint-disable react/jsx-curly-newline */
import React from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import userIcon from '../../../../../assets/permission-user.png';
import EditDeletePopper from '../../../safe/components/EditDeletePopper';
import {
  TitleTwo,
  TitleFour,
  BackgroundColor,
} from '../../../../../styles/GlobalStyles';

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

const Wrap = styled.div``;

const styles = css`
  margin-bottom: 0.5rem;
`;
const permissionStyles = css`
  opacity: 0.7;
`;

const CertificatePermissionsList = (props) => {
  const { onEditClick, list, onDeleteClick, username, userDetails } = props;
  const getUsersDisplayName = (userName) => {
    const obj = userDetails.find(
      (item) => item.userName.toLowerCase() === userName.toLowerCase()
    );
    if (obj) {
      return `${obj.displayName} (${obj.userName})`;
    }
    return username;
  };
  return (
    <UserList>
      {Object.entries(list).map(([key, value]) => {
        return (
          <Wrap key={key}>
            {username !== key && value !== 'write' && (
              <EachUserWrap>
                <IconDetailsWrap>
                  <Icon src={userIcon} alt="user" />
                  <Details>
                    <TitleTwo extraCss={styles}>
                      {getUsersDisplayName(key)}
                    </TitleTwo>
                    <TitleFour extraCss={permissionStyles}>{value}</TitleFour>
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
        );
      })}
    </UserList>
  );
};
CertificatePermissionsList.propTypes = {
  onDeleteClick: PropTypes.func.isRequired,
  onEditClick: PropTypes.func.isRequired,
  list: PropTypes.objectOf(PropTypes.any).isRequired,
  username: PropTypes.string,
  userDetails: PropTypes.arrayOf(PropTypes.any).isRequired,
};

CertificatePermissionsList.defaultProps = {
  username: '',
};

export default CertificatePermissionsList;
