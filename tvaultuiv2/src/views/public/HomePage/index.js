/* eslint-disable react/jsx-one-expression-per-line */
import React from 'react';
import styled, { css } from 'styled-components';
import Union from '../../../assets/Login/union.svg';
import Frame from '../../../assets/Login/frame.svg';
import ButtonComponent from '../../../components/FormFields/ActionButton';
import Speaker from '../../../assets/Login/speaker.png';
import LoginHeaderText from '../../../assets/Login/login-header-text.svg';
import AllGroups from '../../../assets/Login/allgroups.svg';
import Store from '../../../assets/Login/store.svg';
import Access from '../../../assets/Login/access.svg';
import Distribute from '../../../assets/Login/distribute.svg';
import Strings from '../../../resources';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const Container = styled.section`
  padding-top: 11.2rem;
  background-image: linear-gradient(to bottom, #11131b, #2c3040);
`;
const MainContainer = styled.div`
  background: url(${(props) => props.Union || ''});
  background-size: cover;
  background-repeat: no-repeat;
`;
const rowCommonCss = css`
  width: 130rem;
  margin: 0 auto;
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
`;

const SpeakerWrap = styled.img`
  position: absolute;
  width: 71px;
  left: -11px;
  top: -1.45rem;
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
`;

const LeftColumn = styled.div`
  width: 40%;
`;

const Title = styled.h2`
  font-size: 5.6rem;
  font-weight: bold;
  width: 65%;
  margin: 0;
`;

const Description = styled.p`
  line-height: 2.4rem;
  font-size: 1.6rem;
  color: #c4c4c4;
  width: 82%;
  margin: 3rem 0 5rem 0;
`;

const ButtonWrap = styled.div`
  display: flex;
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
`;

const RightColumn = styled.div`
  background: url(${(props) => props.AllGroups || ''});
  background-size: contain;
  background-repeat: no-repeat;
  background-position: right;
  height: 60rem;
  width: 60%;
`;

const SecondRow = styled.div`
  background: url(${(props) => props.Frame || ''});
  background-size: contain;
  background-repeat: no-repeat;
  ${(props) => props.rowCommonCss};
  height: 54rem;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
`;

const CardWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  height: 30rem;
  align-items: flex-end;
  ${(props) => props.rowCommonCss};
`;

const Tile = styled.div`
  height: 21.2rem;
  width:  32%;
  padding: 3rem;
  position: relative;
  background-image: linear-gradient(to top, #11131b, #2c3040);
}
`;

const Image = styled.img`
  position: absolute;
  top: -3rem;
  width: 7rem;
`;
const Heading = styled.h3`
  margin: 3rem 0 2rem;
  font-size: 2.8rem;
`;
const Details = styled.p`
  margin: 0;
  opacity: 0.7;
  font-size: 1.4rem;
`;

const Instruction = styled.p`
  text-align: center;
  font-size: 1.4rem;
  margin: 10rem 0;
  color: rgba(255, 255, 255, 0.7);
  width: 72%;
`;

const ThirdRow = styled.div`
  padding: 2.3rem;
  text-align: center;
  font-size: 1.3rem;
  color: ##c4c4c4;
  a {
    color: #fff;
  }
`;

const LoginPage = () => {
  return (
    <ComponentError>
      <Container>
        <MainContainer Union={Union}>
          <HeaderWrap>
            <SpeakerText>
              <SpeakerWrap src={Speaker} />
              <LoginHeaderTextWrap LoginHeaderText={LoginHeaderText} />
            </SpeakerText>
          </HeaderWrap>
          <FirstRow rowCommonCss={rowCommonCss}>
            <LeftColumn>
              <Title>Welcome to T-Vault</Title>
              <Description>{Strings.Resources.tvaultDescription}</Description>
              <ButtonWrap>
                <ButtonComponent
                  label="Go to Dashboard"
                  color="secondary"
                  onClick={() => {}}
                />
                <SignUp
                  href="https://access.t-mobile.com/manage"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Sign Up
                </SignUp>
              </ButtonWrap>
            </LeftColumn>
            <RightColumn AllGroups={AllGroups} />
          </FirstRow>
          <SecondRow Frame={Frame}>
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
              <strong>Note: </strong>
              {Strings.Resources.loginNotes}
            </Instruction>
          </SecondRow>
          <ThirdRow>
            Developed by Cloud TeamContact us on{' '}
            <a
              target="_blank"
              rel="noopener noreferrer"
              href="https://t-mobile.enterprise.slack.com/?redir=%2Fr-t2678170234%3Fredir%3D%252Fmessages%252FCA5SB94HY"
            >
              Slack
            </a>{' '}
            or shoot us an <a href="mailto: CloudSupport@t-mobile.com">email</a>
          </ThirdRow>
        </MainContainer>
      </Container>
    </ComponentError>
  );
};

export default LoginPage;
