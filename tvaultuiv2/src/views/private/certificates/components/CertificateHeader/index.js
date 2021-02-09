import React from 'react';
import styled, { css } from 'styled-components';
import ReactHtmlParser from 'react-html-parser';
import Strings from '../../../../../resources';
import certIcon from '../../../../../assets/cert-icon.svg';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import CollapsibleDropdown from '../../../../../components/CollapsibleDropdown';

const IconDescriptionWrapper = styled.div`
  display: flex;
  position: relative;
  margin-top: 3.2rem;
`;

const CertIcon = styled.img`
  height: 5.7rem;
  width: 5rem;
  margin-right: 2rem;
`;

const CertDesc = styled.div``;

const Description = styled.p`
  color: #c4c4c4;
  font-size: 1.4rem;
  margin-top: 0;
`;

const InfoLine = styled('p')`
  color: ${(props) => props.theme.customColor.collapse.color};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  strong {
    margin-right: 0.5rem;
  }
  a {
    color: ${(props) => props.theme.customColor.magenta};
  }
`;
const Span = styled.span`
  color: ${(props) => props.theme.customColor.collapse.title};
  fontsize: ${(props) => props.theme.typography.body2.fontSize};
  ${(props) => props.extraStyles}
`;
const CollapsibleContainer = styled.div``;
const ViewMoreStyles = css`
  display: flex;
  align-items: center;
  font-weight: 600;
  cursor: pointer;
  margin-left: 5rem;
`;

const CertificateHeader = () => {
  return (
    <ComponentError>
      <>
        <IconDescriptionWrapper>
          <CertIcon src={certIcon} alt="cert-icon" />
          <CertDesc>
            <Description>{Strings.Resources.certificateDesc}</Description>
          </CertDesc>
        </IconDescriptionWrapper>
        <CollapsibleDropdown
          titleMore="How certificates work"
          titleLess="View Less"
          collapseStyles="background:none"
          titleCss={ViewMoreStyles}
        >
          <CollapsibleContainer>
            <InfoLine>
              <Span>
                <strong>1:</strong>
              </Span>
              {ReactHtmlParser(Strings.Resources.certificateGuide1)}
            </InfoLine>

            <InfoLine>
              <Span>
                <strong>2:</strong>
              </Span>
              {ReactHtmlParser(Strings.Resources.certificateGuide2)}
            </InfoLine>

            <InfoLine>
              <Span>
                <strong>3:</strong>
              </Span>
              {ReactHtmlParser(Strings.Resources.certificateGuide3)}
            </InfoLine>

            <InfoLine>
              <Span>
                <strong>4:</strong>
              </Span>
              {ReactHtmlParser(Strings.Resources.certificateGuide4)}
            </InfoLine>
            <InfoLine>
              <Span>
                <strong>5:</strong>
              </Span>
              {ReactHtmlParser(Strings.Resources.certificateGuide5)}
            </InfoLine>
          </CollapsibleContainer>
        </CollapsibleDropdown>
      </>
    </ComponentError>
  );
};

export default CertificateHeader;
