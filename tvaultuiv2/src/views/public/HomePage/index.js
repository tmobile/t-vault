/* eslint-disable react/jsx-one-expression-per-line */
import React from 'react';
import styled, { css } from 'styled-components';
import Union from '../../../assets/Login/union.svg';
import Frame from '../../../assets/Login/frame.svg';
// import Speaker from '../../assets/Login/speaker.png';
import LoginHeaderText from '../../../assets/Login/login-header-text.svg';
import AllGroups from '../../../assets/Login/allgroups.svg';
import Store from '../../../assets/Login/store.svg';
import Access from '../../../assets/Login/access.svg';
import Distribute from '../../../assets/Login/distribute.svg';
import Strings from '../../../resources';
import ComponentError from '../../../errorBoundaries/ComponentError/component-error';

const Container = styled.section`
  padding-top: 6.5rem;
  background-image: linear-gradient(to bottom, #11131b, #2c3040);
`;

const HeaderWrap = styled.div`
  display: flex;
  justify-content: center;
  width: 100%;
`;

// const SpeakerWrap = styled.div`
//   background: url(${(props) => props.Speaker || ''});
//   background-size: contain;
//   background-repeat: no-repeat;
// `;

const LoginHeaderTextWrap = styled.div`
  background: url(${(props) => props.LoginHeaderText || ''});
  background-size: contain;
  background-repeat: no-repeat;
  height: 5rem;
  width: 60%;
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

const FirstRow = styled.div`
  display: flex;
  align-items: center;
  margin-top: 5.5rem;
  ${(props) => props.rowCommonCss};
`;

const LeftColumn = styled.div`
  width: 50%;
`;

const Title = styled.h2`
  font-size: 5.6rem;
  font-weight: bold;
  width: 50%;
`;

const Description = styled.p`
  line-height: 2.4rem;
  font-size: 1.6rem;
  color: #c4c4c4;
  width: 70%;
`;

const RightColumn = styled.div`
  background: url(${(props) => props.AllGroups || ''});
  background-size: contain;
  background-repeat: no-repeat;
  height: 43rem;
  width: 50%;
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
  top: -3.5rem;
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
            {/* <SpeakerWrap Speaker={Speaker} /> */}
            <LoginHeaderTextWrap LoginHeaderText={LoginHeaderText} />
          </HeaderWrap>
          <FirstRow rowCommonCss={rowCommonCss}>
            <LeftColumn>
              <Title>Welcome to T-Vault</Title>
              <Description>{Strings.Resources.tvaultDescription}</Description>
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
