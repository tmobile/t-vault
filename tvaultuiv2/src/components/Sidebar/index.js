import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { useMatomo } from '@datapunt/matomo-tracker-react';
import { withRouter } from 'react-router-dom';
import Link from '@material-ui/core/Link';
import banner from '../../assets/mob-banner.svg';
import close from '../../assets/close.svg';
import UserLogout from '../Header/userLogout';

const SideMenuWrapper = styled.div`
  width: 33rem;
`;

const BannerCloseWrap = styled.div`
  padding: 4rem;
`;

const Logo = styled.img``;

const Close = styled.img`
  position: absolute;
  right: 1rem;
  top: 1rem;
  cursor: pointer;
`;

const NavItems = styled.div`
  display: flex;
  flex-direction: column;
`;

const NavLink = styled(Link)`
  text-decoration: none;
  padding: 2rem 4rem;
  font-weight: bold;
  background: ${(props) =>
    props.active === 'true' ? props.theme.gradients.sideBar : 'none'};
  :hover {
    text-decoration: none;
  }
`;

const ProfileIconWrap = styled('div')`
  padding-left: 3rem;
  padding-top: 2rem;
`;

const Sidebar = (props) => {
  const {
    onClose,
    navItems,
    currentTab,
    userName,
    checkToken,
    DescriptionIcon,
    EachLink,
  } = props;

  const { trackPageView, trackEvent } = useMatomo();

  useEffect(() => {
    trackPageView();
    return () => {
      trackPageView();
    };
  }, [trackPageView]);

  const handleOnClick = (label) => {
    trackEvent({ category: `${label}-tab`, action: 'click-event' });
  };

  return (
    <SideMenuWrapper>
      <Close src={close} alt="close" onClick={onClose} />
      <BannerCloseWrap>
        <Logo src={banner} alt="banner" />
      </BannerCloseWrap>
      <NavItems>
        {navItems &&
          navItems.map((item) => (
            <NavLink
              href={`/${item.path}`}
              key={item.label}
              active={currentTab === item.path ? 'true' : 'false'}
              onClick={() => handleOnClick(item.path)}
            >
              {item.label}
            </NavLink>
          ))}
      </NavItems>
      <ProfileIconWrap>
        <EachLink
          href="https://docs.corporate.t-mobile.com/t-vault/introduction/"
          target="_blank"
          decoration="none"
        >
          <DescriptionIcon
            style={{ fill: '#c4c4c4', width: '2rem', height: '2rem' }}
          />
          Documentation
        </EachLink>
        <UserLogout userName={userName} checkToken={checkToken} />
      </ProfileIconWrap>
    </SideMenuWrapper>
  );
};

Sidebar.propTypes = {
  onClose: PropTypes.func.isRequired,
  navItems: PropTypes.arrayOf(PropTypes.any).isRequired,
  userName: PropTypes.string.isRequired,
  checkToken: PropTypes.func.isRequired,
  DescriptionIcon: PropTypes.objectOf(PropTypes.any).isRequired,
  EachLink: PropTypes.objectOf(PropTypes.any).isRequired,
  currentTab: PropTypes.string.isRequired,
};
export default withRouter(Sidebar);
