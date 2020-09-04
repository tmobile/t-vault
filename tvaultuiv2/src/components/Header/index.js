import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
// eslint-disable-next-line import/no-unresolved
import ComponentError from '../../errorBoundaries/ComponentError/component-error';

const HeaderWrap = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  font-size: 1.5em;
  padding: 1em;
`;
const HeaderCenter = styled.div`
  display: flex;
`;

// const HeaderLink = ({ path }) => {
//   return <Link to={`/private/dashboard/${path}`}>{path}</Link>;
// };

const NavItem = styled(Link)`
  text-decoration: none;
  margin: 0 0.5em;
  color: ${(props) => props.active.color};
  padding: 0.5em 1em;
  background: ${(props) => props.active.backgroundColor};
  border-radius: 1em;
`;
const ProfileIconWrap = styled('div')`
  width: 20%;
  height: 100%;
  background: #4a4a4a;
  border-radious: 50%;
  align-items: center;
  display: flex;
`;
const Header = () => {
  const navItems = [
    { label: 'Safe', path: 'safe' },
    { label: 'Applications', path: 'applications' },
    { label: 'Service accounts', path: 'service-accounts' },
    { label: 'Certificates', path: 'certificates' },
  ];
  // const { location, theme } = props;
  const [activeNav, setActiveNav] = useState('safe');

  const handleNavChange = (e, item) => {
    setActiveNav(item.label);
  };

  return (
    <ComponentError>
      <HeaderWrap>
        <div>LOGO</div>
        <HeaderCenter>
          {navItems.map((item) => (
            <NavItem
              to={`/${item.path}`}
              key={item.label}
              onClick={(e) => handleNavChange(e, item)}
              active={
                activeNav === item.label
                  ? { color: '#fff', backgroundColor: '#e20074' }
                  : { color: '#4a4a4a', backgroundColor: '#f2f2f2' }
              }
            >
              {item.label}
            </NavItem>
          ))}
        </HeaderCenter>

        <ProfileIconWrap>MS</ProfileIconWrap>
      </HeaderWrap>
    </ComponentError>
  );
};

export default Header;
