import React, { useEffect, useState } from 'react';
import * as msal from '@azure/msal-browser';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import userIcon from '../../assets/icon-profile.svg';
import vectorIcon from '../../assets/vector.svg';
import { revokeToken } from '../../views/public/HomePage/utils';
import configData from '../../config/config';
import mediaBreakpoints from '../../breakpoints';
import configUrl from '../../config/index';

const LogoutWrapper = styled.div`
  position: relative;
  @media (max-width: 1044px) {
    margin-top: 1rem;
  }
`;

const UserWrap = styled.div`
  display: flex;
  align-items: center;
  cursor: pointer;
  ${mediaBreakpoints.smallAndMedium} {
    margin-top: 2rem;
  }
`;

const UserName = styled.div`
  max-width: 10rem;
  text-overflow: ellipsis;
  white-space: nowrap;
  overflow: hidden;
  margin-left: 0.3rem;
`;

const UserIcon = styled.img`
  margin: 0 0.5rem;
`;

const VectorIcon = styled.img`
  margin-left: 0.4rem;
`;

const AdminLabel = styled.div`
  display: flex;
  font-weight: bold;
  color: #fff;
`;

const LogoutPaper = styled.div`
  display: ${(props) => (props.logoutClicked ? 'block' : 'none')};
  position: absolute;
  background-color: #151820;
  padding: 1.5rem 2rem;
  right: 0;
  cursor: pointer;
  font-size: 1.4rem;
  font-weight: bold;
  ${mediaBreakpoints.smallAndMedium} {
    right: 50%;
  }
  :hover {
    background-color: #20232e;
  }
`;

const UserLogout = (props) => {
  const { userName, checkToken } = props;

  const [isAdmin, setIsAdmin] = useState(false);
  const [logoutClicked, setLogoutClicked] = useState(false);

  const handleClick = () => {
    setLogoutClicked(!logoutClicked);
  };

  const onLogoutClicked = async () => {
    setLogoutClicked(false);
    const config = {
      auth: {
        clientId: sessionStorage.getItem('clientId'),
        redirectUri: configUrl.redirectUrl, // defaults to application start page
        postLogoutRedirectUri: configUrl.redirectUrl,
      },
    };

    const myMsal = new msal.PublicClientApplication(config);
    myMsal.logout();
    if (configData.AUTH_TYPE === 'oidc') {
      await revokeToken();
    }
    sessionStorage.clear();
    checkToken();
  };

  useEffect(() => {
    const admin = sessionStorage.getItem('isAdmin');
    if (admin) {
      setIsAdmin(JSON.parse(admin));
    }
  }, []);

  return (
    <LogoutWrapper>
      <UserWrap onClick={() => handleClick()}>
        <UserIcon src={userIcon} alt="usericon" />
        <AdminLabel>
          {isAdmin && <>(Admin) </>}
          <UserName>{userName}</UserName>
        </AdminLabel>
        <VectorIcon src={vectorIcon} alt="vectoricon" />
      </UserWrap>

      <LogoutPaper
        logoutClicked={logoutClicked}
        onClick={() => onLogoutClicked()}
      >
        Logout
      </LogoutPaper>
    </LogoutWrapper>
  );
};

UserLogout.propTypes = {
  userName: PropTypes.string.isRequired,
  checkToken: PropTypes.func.isRequired,
};

export default UserLogout;
