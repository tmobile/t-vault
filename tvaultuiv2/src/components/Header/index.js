/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import styled from 'styled-components';
import { withRouter } from 'react-router-dom';
import PropTypes from 'prop-types';
import Link from '@material-ui/core/Link';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import vaultIcon from '../../assets/tvault.svg';
import userIcon from '../../assets/icon-profile.svg';

const HeaderWrap = styled('div')`
  background-color: #151820;
  box-shadow: 0 5px 25px 0 rgba(226, 0, 116, 0.5);
`;
const Container = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 130rem;
  margin: auto;
  font-size: 1.4rem;
`;

const TVaultIcon = styled.img``;

const HeaderCenter = styled.div`
  display: flex;
`;

const NavLink = styled(Link)`
  text-decoration: none;
  margin: 0 0.5rem;
  color: #fff;
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
`;

const UserName = styled.span``;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const Header = (props) => {
  const { location } = props;
  const [userName] = useState('User');
  const navItems = [
    { label: 'Safe', path: 'safe' },
    { label: 'Applications', path: 'applications' },
    { label: 'Service accounts', path: 'service-accounts' },
    { label: 'Certificates', path: 'certificates' },
  ];

  return (
    <ComponentError>
      <HeaderWrap>
        <Container>
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
