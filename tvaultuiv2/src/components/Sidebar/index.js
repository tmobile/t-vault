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
    configData,
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
    if (label.path !== 'safes') {
      sessionStorage.removeItem('safesApiCount');
    }
  };

  return (
    <SideMenuWrapper>
      <Close src={close} alt="close" onClick={onClose} />
      <BannerCloseWrap>
        <Logo src={banner} alt="banner" />
      </BannerCloseWrap>
      <NavItems>
        {configData.AUTH_TYPE !== 'userpass' && (
          <NavLink
            href="/certificates"
            onClick={() => handleOnClick('Certificates')}
            active={currentTab === 'certificates' ? 'true' : 'false'}
          >
            Certificates
          </NavLink>
        )}
        {navItems &&
          Object.keys(navItems).map((item) =>
            navItems[item].map((nav) => {
              return (
                <NavLink
                  href={`/${nav.path}`}
                  key={nav.label}
                  active={currentTab === nav.path ? 'true' : 'false'}
                  onClick={() => handleOnClick(nav.label)}
                >
                  {nav.label}
                </NavLink>
              );
            })
          )}
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
  configData: PropTypes.objectOf(PropTypes.any).isRequired,
};
export default withRouter(Sidebar);
