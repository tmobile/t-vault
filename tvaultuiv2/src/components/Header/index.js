import React, { useEffect, useState } from 'react';
import styled from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import DescriptionIcon from '@material-ui/icons/Description';
import { useMatomo } from '@datapunt/matomo-tracker-react';
import { withRouter, Link as RRDLink } from 'react-router-dom';
import PropTypes from 'prop-types';
import Link from '@material-ui/core/Link';
import SwipeableDrawer from '@material-ui/core/SwipeableDrawer';
import ComponentError from '../../errorBoundaries/ComponentError/component-error';
import mediaBreakpoints from '../../breakpoints';
import vaultIcon from '../../assets/tvault.svg';
import menu from '../../assets/menu.svg';
import Sidebar from '../Sidebar';
import UserLogout from './userLogout';
import configData from '../../config/config';
import configUrl from '../../config';

const { small, smallAndMedium, semiLarge } = mediaBreakpoints;

const HeaderWrap = styled('header')`
  background-color: #151820;
  box-shadow: 0 5px 25px 0 rgba(226, 0, 116, 0.5);
  position: fixed;
  top: 0;
  width: 100%;
  z-index: 10;
  @media (max-width: 1044px) {
    box-shadow: 0 5px 15px 0 rgba(226, 0, 116, 0.5);
  }
`;
const Container = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 130rem;
  margin: auto;
  font-size: 1.4rem;
  height: 7rem;
  ${semiLarge} {
    margin: 0 3.5rem;
  }
  @media (max-width: 1044px) {
    justify-content: ${(props) => (props.isLogin ? 'center' : 'space-between')};
    padding: 0 2rem;
  }
`;

const MenuIcon = styled.img`
  display: none;
  @media (max-width: 1044px) {
    display: block;
    position: absolute;
    left: 3.5rem;
    cursor: pointer;
  }
  ${small} {
    left: 2rem;
  }
`;

const TVaultIcon = styled.img`
  margin-right: 5rem;
  ${smallAndMedium} {
    margin-right: 0;
  }
`;

const HeaderCenter = styled.div`
  display: flex;
  @media (max-width: 1044px) {
    display: none;
  }
`;

const NavLink = styled(Link)`
  text-decoration: none;
  padding: 2.5rem 1rem;
  font-size: 1.3rem;
  font-weight: bold;
  background: ${(props) =>
    props.active === 'true' ? props.theme.gradients.nav : 'none'};
  :hover {
    text-decoration: none;
  }
`;

const DocLinks = styled.div`
  display: flex;
`;
const ProfileIconWrap = styled('div')`
  display: flex;
  align-items: center;
  margin-left: auto;
  @media (max-width: 1044px) {
    display: none;
  }
`;
const EachLink = styled.a`
  margin: 0 1rem;
  color: #fff;
  font-size: 1.4rem;
  display: flex;
  align-items: center;
  font-weight: bold;
  text-decoration: underline;
  @media (max-width: 768px) {
    margin: 0 0.5rem;
  }
  svg {
    margin-right: 0.5rem;
  }
`;

const useStyles = makeStyles(() => ({
  root: {
    '& .MuiDrawer-paper': {
      boxShadow: '5px 0 15px 0 rgba(226, 0, 116, 0.5)',
      backgroundColor: '#151820',
    },
  },
}));

const Header = (props) => {
  const classes = useStyles();
  const [isLogin, setIsLogin] = useState(false);
  const [currentTab, setCurrentTab] = useState('Safes');
  const { location } = props;
  const [navItems, setNavItems] = useState([]);
  const [userName, setUserName] = useState('User');
  const [state, setState] = useState({
    left: false,
  });

  const generalNavItems = [
    { label: 'Safes', path: 'safes' },
    { label: 'Vault AppRoles', path: 'vault-app-roles' },
    { label: 'Service Accounts', path: 'service-accounts' },
    { label: 'Certificates', path: 'certificates' },
    { label: 'IAM Service Accounts', path: 'iam-service-accounts' },
    { label: 'Azure Principal', path: 'azure-principal' },
  ];

  const userPassNavItems = [
    { label: 'Safes', path: 'safes' },
    { label: 'Vault AppRoles', path: 'vault-app-roles' },
    { label: 'Service Accounts', path: 'service-accounts' },
  ];

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

  useEffect(() => {
    if (configData.AUTH_TYPE === 'userpass') {
      setNavItems([...userPassNavItems]);
    } else {
      setNavItems([...generalNavItems]);
    }
    // eslint-disable-next-line
  }, []);

  const hideSideMenu = (anchor, open) => {
    setState({ ...state, [anchor]: open });
  };

  const checkToken = () => {
    if (window.location.pathname === '/') {
      setIsLogin(false);
    } else {
      setIsLogin(true);
    }
    const name = sessionStorage.getItem('username');
    if (name !== null) {
      setUserName(`${name}` || 'User');
    }
  };

  useEffect(() => {
    checkToken();
    const path = location.pathname.split('/');
    setCurrentTab(path[1]);
  }, [location]);

  return (
    <ComponentError>
      <HeaderWrap>
        <Container isLogin={isLogin}>
          {isLogin && (
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
                className={classes.root}
              >
                <Sidebar
                  onClose={() => hideSideMenu('left', false)}
                  navItems={navItems}
                  userName={userName}
                  checkToken={checkToken}
                  EachLink={EachLink}
                  DescriptionIcon={DescriptionIcon}
                  currentTab={currentTab}
                />
              </SwipeableDrawer>
            </>
          )}

          <TVaultIcon src={vaultIcon} alt="tvault-logo" />
          {isLogin && (
            <HeaderCenter>
              {navItems &&
                navItems.map((item) => (
                  <NavLink
                    key={item.label}
                    to={`/${item.path}`}
                    onClick={() => handleOnClick(item.path)}
                    component={RRDLink}
                    active={currentTab === item.path ? 'true' : 'false'}
                  >
                    {item.label}
                  </NavLink>
                ))}
            </HeaderCenter>
          )}
          <>
            {!isLogin ? (
              <DocLinks>
                <EachLink
                  href={configData.DOCS_LINK}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Docs
                </EachLink>
                <EachLink
                  href={`${configUrl.baseUrl.replace(
                    '/vault/v2',
                    ''
                  )}/vault/swagger-ui.html`}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Developer API
                </EachLink>
              </DocLinks>
            ) : (
              <ProfileIconWrap>
                <EachLink
                  href={`${configUrl.baseUrl.replace(
                    '/vault/v2',
                    ''
                  )}/vault/swagger-ui.html`}
                  target="_blank"
                  rel="noopener noreferrer"
                  decoration="none"
                >
                  <DescriptionIcon
                    style={{ fill: '#c4c4c4', width: '2rem', height: '2rem' }}
                  />
                  Documentation
                </EachLink>
                <UserLogout userName={userName} checkToken={checkToken} />
              </ProfileIconWrap>
            )}
          </>
        </Container>
      </HeaderWrap>
    </ComponentError>
  );
};

Header.propTypes = {
  location: PropTypes.objectOf(PropTypes.any).isRequired,
};

export default withRouter(Header);
