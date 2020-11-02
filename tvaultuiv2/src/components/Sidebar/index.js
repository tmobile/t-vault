import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { withRouter } from 'react-router-dom';
import Link from '@material-ui/core/Link';
import banner from '../../assets/mob-banner.svg';
import close from '../../assets/close.svg';
import UserLogout from '../Header/userLogout';

const SideMenuWrapper = styled.div`
  width: 33rem;
`;

const BannerCloseWrap = styled.div`
  display: flex;
  align-item: center;
  justify-content: space-between;
  padding: 4rem 3rem 5.5rem 4rem;
`;

const Logo = styled.img``;

const Close = styled.img``;

const NavItems = styled.div`
  display: flex;
  flex-direction: column;
  margin: 2rem 0;
`;

const NavLink = styled(Link)`
  text-decoration: none;
  padding: 2rem 4rem;
  font-weight: bold;
  background: ${(props) =>
    props.active ? props.theme.gradients.sideBar : 'none'};
  :hover {
    text-decoration: none;
  }
`;

const ProfileIconWrap = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 4rem;
  padding-top: 2rem;
`;

const Sidebar = (props) => {
  const { onClose, navItems, location, userName, checkToken } = props;
  return (
    <SideMenuWrapper>
      <BannerCloseWrap>
        <Logo src={banner} alt="banner" />
        <Close src={close} alt="close" onClick={onClose} />
      </BannerCloseWrap>
      <NavItems>
        {navItems &&
          navItems.map((item) => (
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
        <UserLogout userName={userName} checkToken={checkToken} />
      </ProfileIconWrap>
    </SideMenuWrapper>
  );
};

Sidebar.propTypes = {
  onClose: PropTypes.func.isRequired,
  navItems: PropTypes.arrayOf(PropTypes.any).isRequired,
  location: PropTypes.objectOf(PropTypes.any).isRequired,
  userName: PropTypes.string.isRequired,
  checkToken: PropTypes.func.isRequired,
};
export default withRouter(Sidebar);
