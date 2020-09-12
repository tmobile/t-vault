/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import styled from 'styled-components';
import { withRouter } from 'react-router-dom';
import PropTypes from 'prop-types';
import Link from '@material-ui/core/Link';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from 'breakpoints';
import vaultIcon from 'assets/tvault.svg';
import menu from 'assets/menu.svg';
import userIcon from 'assets/icon-profile.svg';
import SwipeableDrawer from '@material-ui/core/SwipeableDrawer';
import Sidebar from '../Sidebar';

const { small, smallAndMedium } = mediaBreakpoints;

const HeaderWrap = styled('div')`
  background-color: #151820;
  box-shadow: 0 5px 25px 0 rgba(226, 0, 116, 0.5);
`;
const Container = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 130rem;
  margin: auto;
  font-size: 1.4rem;
  height: 7rem;
  ${smallAndMedium} {
    justify-content: center;
  }
`;

const MenuIcon = styled.img`
  display: none;
  ${smallAndMedium} {
    display: block;
    position: absolute;
    left: 3.5rem;
    cursor: pointer;
  }
  ${small} {
    left: 2rem;
  }
`;

const TVaultIcon = styled.img``;

const HeaderCenter = styled.div`
  display: flex;
  ${smallAndMedium} {
    display: none;
  }
`;

const NavLink = styled(Link)`
  text-decoration: none;
  margin: 0 0.5rem;
  padding: 2.5rem 2rem;
  font-weight: bold;
  background: ${(props) => (props.active ? props.theme.gradients.nav : 'none')};
  :hover {
    text-decoration: none;
  }
`;
const ProfileIconWrap = styled('div')`
  display: flex;
  align-items: center;
  ${smallAndMedium} {
    display: none;
  }
`;

const UserName = styled.span``;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const Header = (props) => {
  const { location } = props;
  const [userName] = useState('User');
  const [state, setState] = React.useState({
    left: false,
  });

  const toggleDrawer = (anchor, open) => (event) => {
    if (
      event &&
      event.type === 'keydown' &&
      (event.key === 'Tab' || event.key === 'Shift')
    ) {
      return;
    }
    setState({ ...state, [anchor]: open });
  };
  const navItems = [
    { label: 'Safe', path: 'safe' },
    { label: 'Applications', path: 'applications' },
    { label: 'Service accounts', path: 'service-accounts' },
    { label: 'Certificates', path: 'certificates' },
  ];

  const hideSideMenu = (anchor, open) => {
    setState({ ...state, [anchor]: open });
  };

  return (
    <ComponentError>
      <HeaderWrap>
        <Container>
          <>
            <MenuIcon
              src={menu}
              alt="menu"
              onClick={toggleDrawer('left', true)}
            />
            <SwipeableDrawer
              anchor="left"
              open={state.left}
              onClose={toggleDrawer('left', false)}
              onOpen={toggleDrawer('left', true)}
            >
              <Sidebar
                onClose={() => hideSideMenu('left', false)}
                navItems={navItems}
                userName={userName}
              />
            </SwipeableDrawer>
          </>

          <TVaultIcon src={vaultIcon} alt="tvault-logo" />
          <HeaderCenter>
            {navItems.map((item) => (
              <NavLink
                href={`/${item.path}`}
                key={item.label}
                active={`/${location.pathname}`.includes(item.path)}
              >
                {item.label}
              </NavLink>
            ))}
          </HeaderCenter>
          <ProfileIconWrap>
            <UserName>{userName}</UserName>
            <UserIcon src={userIcon} alt="usericon" />
          </ProfileIconWrap>
        </Container>
      </HeaderWrap>
    </ComponentError>
  );
};

Header.propTypes = {
  location: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default withRouter(Header);
