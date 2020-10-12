import React from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import userIcon from '../../../../../../../assets/permission-user.png';
import PopperElement from '../../../Popper';
import {
  TitleTwo,
  TitleFour,
  BackgroundColor,
} from '../../../../../../../styles/GlobalStyles';
import {
  IconDeleteActive,
  IconEdit,
} from '../../../../../../../assets/SvgIcons';

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
const PermissionsList = (props) => {
  const { onEditClick, list, onDeleteClick } = props;
  return (
    <UserList>
      {Object.entries(list).map(([key, value]) => (
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
  );
};
PermissionsList.propTypes = {
  onDeleteClick: PropTypes.func.isRequired,
  onEditClick: PropTypes.func.isRequired,
  list: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default PermissionsList;
