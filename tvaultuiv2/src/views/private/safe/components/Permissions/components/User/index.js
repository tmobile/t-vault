/* eslint-disable import/no-unresolved */
import React from 'react';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import NoData from 'components/NoData';
import ButtonComponent from 'components/FormFields/ActionButton';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import { TitleTwo, TitleFour, BackgroundColor } from 'styles/GlobalStyles';
import { IconDeleteActive, IconEdit } from 'assets/SvgIcons';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import noPermissionsIcon from 'assets/no-permissions.svg';
import userIcon from 'assets/permission-user.png';
import mediaBreakpoints from 'breakpoints';
import PopperElement from '../../../Popper';
import AddUser from '../../../AddUser';

const { small } = mediaBreakpoints;

const NoDataWrapper = styled.section`
  display: flex;
  justify-content: center;
  margin-top: 2.5rem;
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

  const isMobileScreen = useMediaQuery(small);
  const onSubmit = (search, radio) => {
    const value = {
      name: search,
      last_updated: '2days ago',
      permission: radio,
    };
    onSaveClicked(value);
  };
  return (
    <ComponentError>
      {addPermission ? (
        <AddUser
          handleSaveClick={(search, radio) => onSubmit(search, radio)}
          handleCancelClick={onCancelClicked}
        />
      ) : (
        ''
      )}
      {users && users.length > 0 && !addPermission ? (
        <UserList>
          {users.map((item) => {
            return (
              <EachUserWrap key={item.name}>
                <IconDetailsWrap>
                  <Icon src={userIcon} alt="user" />
                  <Details>
                    <TitleTwo extraCss={styles}>{item.name}</TitleTwo>
                    <TitleFour extraCss={permissionStyles}>
                      {item.last_updated}
                      {' - '}
                      {item.permission}
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
                    <PopperItem>
                      <IconEdit />
                      <span>Edit</span>
                    </PopperItem>
                    <PopperItem>
                      <IconDeleteActive />
                      <span> Delete</span>
                    </PopperItem>
                  </PopperElement>
                </FolderIconWrap>
              </EachUserWrap>
            );
          })}
        </UserList>
      ) : (
        !addPermission && (
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
    </ComponentError>
  );
};

User.propTypes = {
  users: PropTypes.arrayOf(PropTypes.any).isRequired,
  onSaveClicked: PropTypes.func.isRequired,
  addPermission: PropTypes.bool.isRequired,
  onNoDataAddClicked: PropTypes.func.isRequired,
  onCancelClicked: PropTypes.func.isRequired,
};
export default User;
