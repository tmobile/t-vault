import React from 'react';
import { Link } from 'react-router-dom';
import styled, { css } from 'styled-components';

const HeaderWrap = styled('div')`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ddd;
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
  color: #000;
  padding: 0.5em 1em;
  background: #eee;
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
  const navItems = ['safe', 'applications', 'service accounts', 'certificates'];
  return (
    <HeaderWrap>
      <div>LOGO</div>
      <HeaderCenter>
        {navItems.map((item, i) => (
          <NavItem to={`/private/${item}`} key={i}>
            {item}
          </NavItem>
        ))}
      </HeaderCenter>

      <ProfileIconWrap>MS</ProfileIconWrap>
    </HeaderWrap>
  );
};

export default Header;
