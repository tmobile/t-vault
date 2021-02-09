/* eslint-disable react/jsx-curly-newline */
/* eslint-disable react/jsx-one-expression-per-line */
import React, { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import queryString from 'query-string';
import { useLocation } from 'react-router-dom';
import axios from 'axios';
import Union from '../../../assets/Login/background.svg';
import IpadBackground from '../../../assets/Login/ipad-background.svg';
import MobBackground from '../../../assets/Login/mob-background.svg';
import Rectangle from '../../../assets/Login/rectangle.svg';
import IpadRectangle from '../../../assets/Login/ipad-rectangle.svg';
import MobRectangle from '../../../assets/Login/mob-rectangle.svg';
import ButtonComponent from '../../../components/FormFields/ActionButton';
import Speaker from '../../../assets/Login/speaker.png';
import LoginHeaderText from '../../../assets/Login/login-header-text.svg';
import AllGroups from '../../../assets/Login/allgroups.svg';
import Store from '../../../assets/Login/store.svg';
import Access from '../../../assets/Login/access.svg';
import Distribute from '../../../assets/Login/distribute.svg';
import Strings from '../../../resources';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';
import { useStateValue } from '../../../contexts/globalState';
import mediaBreakpoints from '../../../breakpoints';
import apiService from './apiService';
import Loader from '../../../components/Loaders/ScaledLoader';
import configUrl from '../../../config';
import configData from '../../../config/config';
import LoginModal from './LoginModal';
import SnackbarComponent from '../../../components/Snackbar';
import { renewToken } from './utils';

const { smallAndMedium, small } = mediaBreakpoints;

const LoaderWrap = styled.div`
  height: 100vh;
  background: linear-gradient(to top, #11131b, #2c3040);
`;

const Container = styled.section`
  padding-top: 11.2rem;
  background-image: url(${(props) => props.Rectangle || ''}),
    linear-gradient(to top, #11131b, #2c3040);
  background-size: cover;
  background-repeat: no-repeat;
  @media (max-width: 1024px) {
    background-image: url(${(props) => props.IpadRectangle || ''}),
      linear-gradient(to top, #11131b, #2c3040);
    background-size: cover;
    background-repeat: no-repeat;
  }
  ${small} {
    background-image: url(${(props) => props.MobRectangle || ''}),
      linear-gradient(to top, #11131b, #2c3040);
    background-size: cover;
    background-repeat: no-repeat;
  }
`;

const MainContainer = styled.div`
  background: url(${(props) => props.Union || ''});
  background-size: cover;
  background-repeat: no-repeat;
  @media (max-width: 1024px) {
    background: none;
  }
`;
const rowCommonCss = css`
  width: 130rem;
  margin: 0 auto;
  @media (max-width: 1320px) {
    width: 120rem;
  }
  @media (max-width: 1024px) {
    width: auto;
    padding: 0 3rem;
  }
  ${small} {
    padding: 0 2rem;
  }
`;

const HeaderWrap = styled.div`
  margin-top: 2rem;
  width: 100%;
  height: 5rem;
  display: flex;
  justify-content: center;
`;

const SpeakerText = styled.div`
  position: absolute;
  display: flex;
  justify-content: center;
  width: 50%;
  ${smallAndMedium} {
    width: 90%;
  }
`;

const SpeakerWrap = styled.img`
  position: absolute;
  width: 71px;
  left: -11px;
  top: -1.45rem;
  ${small} {
    width: 40px;
    left: -5px;
    top: -1rem;
  }
`;

const LoginHeaderTextWrap = styled.div`
  background: url(${(props) => props.LoginHeaderText || ''});
  background-size: contain;
  background-repeat: no-repeat;
  height: 5rem;
  width: 100%;
`;

const FirstRow = styled.div`
  display: flex;
  align-items: center;
  height: 65rem;
  ${(props) => props.rowCommonCss};
  @media (max-width: 1024px) {
    flex-direction: column;
    height: auto;
    margin-top: 5.5rem;
  }
  ${small} {
    margin-top: 2rem;
  }
`;

const LeftColumn = styled.div`
  width: 40%;
  @media (max-width: 1024px) {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
    text-align: center;
  }
`;

const Title = styled.h2`
  font-size: 5.6rem;
  font-weight: bold;
  width: 65%;
  margin: 0;
  @media (max-width: 1024px) {
    width: 45%;
    font-size: 4.8rem;
  }
  ${small} {
    font-size: 4rem;
    width: 80%;
  }
`;

const Description = styled.p`
  line-height: 2.4rem;
  font-size: 1.6rem;
  color: #c4c4c4;
  width: 82%;
  margin: 3rem 0 5rem 0;
  @media (max-width: 1024px) {
    width: 75%;
  }
  ${small} {
    width: 100%;
  }
`;

const ButtonWrap = styled.div`
  display: flex;
  ${small} {
    margin-bottom: 3rem;
    width: 100%;
  }
`;

const SignUp = styled.a`
  background-color: #fff;
  margin-left: 1rem;
  text-decoration: none;
  font-size: 1.4rem;
  color: #e20074;
  font-weight: bold;
  height: 3.6rem;
  display: flex;
  align-items: center;
  width: 10rem;
  justify-content: center;
  ${small} {
    width: 100%;
    height: 4.5rem;
  }
`;

const RightColumn = styled.div`
  background: url(${(props) => props.AllGroups || ''});
  background-size: contain;
  background-repeat: no-repeat;
  background-position: right;
  height: 60rem;
  width: 60%;
  @media (max-width: 1024px) {
    display: none;
  }
`;

const SecondRow = styled.div`
  ${(props) => props.rowCommonCss};
  height: 70rem;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  @media (max-width: 1024px) {
    height: auto;
    background: url(${(props) => props.IpadBackground || ''});
    background-size: cover;
    background-repeat: no-repeat;
  }
  ${small} {
    background: url(${(props) => props.MobBackground || ''});
    background-size: cover;
    background-repeat: no-repeat;
  }
`;

const TabAllGroups = styled.div`
  display: none;
  @media (max-width: 1024px) {
    display: block;
    background: url(${(props) => props.AllGroups || ''});
    background-size: contain;
    background-repeat: no-repeat;
    background-position: center;
    height: 50rem;
    width: 60%;
  }
  ${small} {
    height: 20rem;
  }
`;

const CardWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  height: 40rem;
  align-items: flex-end;
  ${(props) => props.rowCommonCss};
  @media (max-width: 1024px) {
    flex-direction: column;
    align-items: center;
    height: auto;
    margin-top: 4rem;
  }
  ${small} {
    margin-top: 10rem;
  }
`;

const Tile = styled.div`
  height: 21.2rem;
  width:  32%;
  padding: 3rem;
  position: relative;
  background-image: linear-gradient(to top, #11131b, #2c3040);
  @media (max-width: 1320px) {
    height: 25rem;
  }
  @media (max-width: 1024px) {
    width: 100%;
    margin: 2.5rem 0;
    height: 20rem;
  }
  ${small}{
    height: 22rem;
  }
}
`;

const Image = styled.img`
  position: absolute;
  top: -3rem;
  width: 7rem;
  ${small} {
    width: 6rem;
  }
`;
const Heading = styled.h3`
  margin: 3rem 0 2rem;
  font-size: 2.8rem;
  ${small} {
    margin: 2rem 0 1.2rem;
  }
`;
const Details = styled.p`
  margin: 0;
  opacity: 0.7;
  font-size: 1.4rem;
`;

const Instruction = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 1.4rem;
  height: calc(100% - 40rem);
  color: rgba(255, 255, 255, 0.7);
  width: 72%;
  span {
    color: #fff;
    font-weight: bold;
    display: contents;
  }
  @media (max-width: 1024px) {
    height: auto;
    margin: 4rem 0;
  }
`;

const ThirdRow = styled.div`
  height: 11.3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  font-size: 1.3rem;
  color: ##c4c4c4;
  background-color: #2c3040;
  a {
    color: #fff;
    margin: 0 0.3rem;
  }
`;

const ContactUs = styled.p`
  ${small} {
    width: 80%;
  }
`;

const LoginPage = () => {
  const [response, setResponse] = useState({ status: 'home' });
  const [openLoginModal, setOpenLoginModal] = useState(false);
  const [responseType, setResponseType] = useState(null);
  const [toastMessage, setToastMessage] = useState('');
  const [, dispatch] = useStateValue();
  const isMobileScreen = useMediaQuery(small);

  const { search } = useLocation();
  const urlParams = queryString.parse(search);

  const checkAdmin = (value) => {
    if (value === 'yes') {
      sessionStorage.setItem('isAdmin', true);
    } else {
      sessionStorage.setItem('isAdmin', false);
    }
  };

  const getOwnerAllDetails = (loggedInUser) => {
    return apiService
      .getOwnerDetails(loggedInUser)
      .then((res) => {
        if (res.data.data.values && res.data.data.values[0]) {
          if (res.data.data.values[0].userEmail) {
            sessionStorage.setItem('owner', res.data.data.values[0].userEmail);
            sessionStorage.setItem(
              'displayName',
              res.data.data.values[0].displayName.toLowerCase()
            );
          }
        }
      })
      .catch(() => {
        setResponseType(-1);
        setResponse({ status: 'home' });
        setToastMessage('Something went wrong while fetching owner details!');
      });
  };

  const getLoggedInUserName = () => {
    return apiService
      .getUserName()
      .then(async (res) => {
        if (res.data && res.data.data?.username) {
          sessionStorage.setItem(
            'username',
            res.data.data.username.toLowerCase()
          );
          await getOwnerAllDetails(res.data.data.username.toLowerCase());
        }
      })
      .catch(() => {
        setResponseType(-1);
        setResponse({ status: 'home' });
        setToastMessage('Something went wrong while fetching username!');
      });
  };

  useEffect(() => {
    if (urlParams?.code && urlParams?.state) {
      sessionStorage.clear();
      setResponse({ status: 'loading' });
      axios
        .get(
          `${configUrl.baseUrl}/auth/oidc/callback?state=${urlParams.state}&code=${urlParams.code}`
        )
        .then(async (res) => {
          if (res?.data) {
            setResponse({ status: 'loading' });
            sessionStorage.setItem('token', res.data.client_token);
            checkAdmin(res?.data?.admin);
            await getLoggedInUserName();
            await renewToken();
            dispatch({ type: 'CALLBACK_DATA', payload: { ...res.data } });
            window.location = '/safes';
          }
        })
        .catch(() => {
          setResponseType(-1);
          setResponse({ status: 'home' });
        });
    }
    // eslint-disable-next-line
  }, []);

  const onDashboardClicked = () => {
    if (configData.AUTH_TYPE === 'oidc') {
      setResponse({ status: 'loading' });
      const payload = {
        role: 'default',
        redirect_uri: configUrl.redirectUrl,
      };
      axios
        .post(`${configUrl.baseUrl}/auth/oidc/auth_url`, payload)
        .then((res) => {
          window.location = res.data?.data?.auth_url;
        })
        .catch(() => {
          setResponseType(null);
          setResponse({ status: 'home' });
        });
    } else {
      setOpenLoginModal(true);
    }
  };
  const closeLoginModal = () => {
    setOpenLoginModal(false);
  };

  const ldapApiCall = (payload) => {
    axios
      .post(`${configUrl.baseUrl}/auth/tvault/login`, payload)
      .then(async (res) => {
        if (res?.data) {
          sessionStorage.setItem('token', res.data.client_token);
          checkAdmin(res?.data?.admin);
          sessionStorage.setItem('access', JSON.stringify(res.data.access));
          sessionStorage.setItem('username', payload.username.toLowerCase());
          await getOwnerAllDetails(payload.username.toLowerCase());
          window.location = '/safes';
        }
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err?.response?.data?.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
        setResponse({ status: 'home' });
      });
  };

  const userpassApiCall = (payload) => {
    axios
      .post(`${configUrl.baseUrl}/auth/tvault/login`, payload)
      .then(async (res) => {
        sessionStorage.setItem('token', res.data.client_token);
        checkAdmin(res?.data?.admin);
        sessionStorage.setItem('access', JSON.stringify(res.data.access));
        sessionStorage.setItem('username', payload.username.toLowerCase());
        await getOwnerAllDetails(payload.username.toLowerCase());
        window.location = '/safes';
      })
      .catch((err) => {
        if (err?.response?.data?.errors && err.response.data.errors[0]) {
          setToastMessage(err.response.data.errors[0]);
        }
        setResponseType(-1);
        setResponse({ status: 'home' });
      });
  };

  const onSignInClicked = (username, password) => {
    setResponse({ status: 'loading' });
    setOpenLoginModal(false);
    const payload = {
      username,
      password,
    };
    if (configData.AUTH_TYPE === 'userpass') {
      userpassApiCall(payload);
    } else {
      ldapApiCall(payload);
    }
  };

  const onToastClose = (reason) => {
    if (reason === 'clickaway') {
      return;
    }
    setResponseType(null);
  };

  return (
    <ComponentError>
      <>
        <LoginModal
          handleClose={() => closeLoginModal()}
          open={openLoginModal}
          onSignInClicked={(username, password) =>
            onSignInClicked(username, password)
          }
        />
        {response.status === 'loading' && (
          <LoaderWrap>
            <Loader />
          </LoaderWrap>
        )}
        {response.status === 'home' && (
          <Container
            Rectangle={Rectangle}
            IpadRectangle={IpadRectangle}
            MobRectangle={MobRectangle}
          >
            <MainContainer Union={Union}>
              <HeaderWrap>
                <SpeakerText>
                  <SpeakerWrap src={Speaker} />
                  <LoginHeaderTextWrap LoginHeaderText={LoginHeaderText} />
                </SpeakerText>
              </HeaderWrap>
              <FirstRow rowCommonCss={rowCommonCss}>
                <LeftColumn>
                  <Title>Welcome To T-Vault</Title>
                  <Description>
                    {Strings.Resources.tvaultDescription}
                  </Description>
                  <ButtonWrap>
                    <ButtonComponent
                      label="Go to Dashboard"
                      color="secondary"
                      onClick={() => onDashboardClicked()}
                      width={isMobileScreen ? '100%' : ''}
                    />
                    <SignUp
                      href={configData.SIGN_UP_LINK}
                      target="_blank"
                      rel="noopener noreferrer"
                    >
                      Sign Up
                    </SignUp>
                  </ButtonWrap>
                </LeftColumn>
                <RightColumn AllGroups={AllGroups} />
              </FirstRow>
              <SecondRow
                IpadBackground={IpadBackground}
                MobBackground={MobBackground}
              >
                <TabAllGroups AllGroups={AllGroups} />
                <CardWrapper rowCommonCss={rowCommonCss}>
                  <Tile>
                    <Image src={Store} alt="store" />
                    <Heading>Store</Heading>
                    <Details>{Strings.Resources.storeDescription}</Details>
                  </Tile>
                  <Tile>
                    <Image src={Access} alt="access" />
                    <Heading>Access</Heading>
                    <Details>{Strings.Resources.accessDescription}</Details>
                  </Tile>
                  <Tile>
                    <Image src={Distribute} alt="distribute" />
                    <Heading>Distribute</Heading>
                    <Details>{Strings.Resources.distributeDescription}</Details>
                  </Tile>
                </CardWrapper>
                <Instruction>
                  <span>Note: </span>
                  {Strings.Resources.loginNotes}
                </Instruction>
              </SecondRow>
            </MainContainer>
            <ThirdRow>
              <ContactUs>
                Developed by Cloud TeamContact us on
                <a
                  target="_blank"
                  rel="noopener noreferrer"
                  href={configData.SLACK_LINK}
                >
                  Slack
                </a>
                or shoot us an <a href={configData.EMAIL_LINK}>email</a>
              </ContactUs>
            </ThirdRow>
          </Container>
        )}
        {responseType === -1 && (
          <SnackbarComponent
            open
            onClose={() => onToastClose()}
            severity="error"
            icon="error"
            message={toastMessage || 'Something went wrong!'}
          />
        )}
      </>
    </ComponentError>
  );
};

export default LoginPage;
