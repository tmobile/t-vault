import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { withRouter } from 'react-router-dom';
import Link from '@material-ui/core/Link';
import userIcon from '../../assets/icon-profile.svg';
import banner from '../../assets/mob-banner.svg';
import close from '../../assets/close.svg';

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

const UserName = styled.span``;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const Sidebar = (props) => {
  const { onClose, navItems, location, userName } = props;
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
        <UserName>{userName}</UserName>
        <UserIcon src={userIcon} alt="usericon" />
      </ProfileIconWrap>
    </SideMenuWrapper>
  );
};

Sidebar.propTypes = {
  onClose: PropTypes.func.isRequired,
  navItems: PropTypes.arrayOf(PropTypes.any).isRequired,
  location: PropTypes.objectOf(PropTypes.any).isRequired,
  userName: PropTypes.string.isRequired,
};
export default withRouter(Sidebar);
