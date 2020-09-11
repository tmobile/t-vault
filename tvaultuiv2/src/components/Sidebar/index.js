/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { withRouter } from 'react-router-dom';
import Link from '@material-ui/core/Link';
import userIcon from 'assets/icon-profile.svg';
import ButtonComponent from '../FormFields/ActionButton';

const SideMenuWrapper = styled.div`
  padding: 2rem 0;
`;

const NavItems = styled.div`
  display: flex;
  flex-direction: column;
  margin: 2rem 0;
`;

const NavLink = styled(Link)`
  text-decoration: none;
  padding: 2rem;
  font-weight: bold;
  background: ${(props) =>
    props.active ? props.theme.gradients.list : 'none'};
  :hover {
    text-decoration: none;
  }
`;

const ProfileIconWrap = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 2rem;
`;

const UserName = styled.span``;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const Sidebar = (props) => {
  const { hideSideMenu, navItems, location, userName } = props;
  return (
    <SideMenuWrapper>
      <ButtonComponent color="primary" onClick={hideSideMenu} label="Close" />
      <NavItems>
        {navItems.map((item) => (
          <NavLink
            href={`/${item.path}`}
            key={item.label}
            active={`/${location.pathname}`.includes(item.path)}
          >
            {item.label}
          </NavLink>
        ))}
      </NavItems>
      <ProfileIconWrap>
        <UserName>{userName}</UserName>
        <UserIcon src={userIcon} alt="usericon" />
      </ProfileIconWrap>
    </SideMenuWrapper>
  );
};

Sidebar.propTypes = {
  hideSideMenu: PropTypes.func.isRequired,
  navItems: PropTypes.arrayOf(PropTypes.any).isRequired,
  location: PropTypes.objectOf(PropTypes.any).isRequired,
  userName: PropTypes.string.isRequired,
};
export default withRouter(Sidebar);
