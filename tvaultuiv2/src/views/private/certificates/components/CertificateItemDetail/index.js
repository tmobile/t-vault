import React from 'react';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled, { css } from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { BackArrow } from '../../../../../assets/SvgIcons';
import mediaBreakpoints from '../../../../../breakpoints';
import { TitleOne, TitleThree } from '../../../../../styles/GlobalStyles';

// styled components goes here
const Section = styled('section')`
  flex-direction: column;
  display: flex;
  z-index: 2;
  width: 100%;
  height: 100%;
`;

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  position: relative;
  height: 17.1rem;
  padding: 2rem;
  .list-title-wrap {
    width: 70%;
    z-index: 2;
  }
  ${mediaBreakpoints.small} {
    height: 18rem;
    padding: 1rem;
  }
`;

const BackButton = styled.div`
  display: flex;
  align-items: center;
  padding: 2rem 0 0 2rem;

  ${mediaBreakpoints.small} {
    position: absolute;
    z-index: 2;
  }
`;

const HeaderBg = styled('div')`
  position: absolute;
  top: -0.8rem;
  left: 0;
  right: 0;
  bottom: 0;
  background: url(${(props) => props.bgImage || ''});
  ${mediaBreakpoints.small} {
    z-index: -1;
  }
`;

const ListTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography};
  margin: 1rem 0 1.2rem;
  text-overflow: ellipsis;
  overflow: hidden;
`;

const DetailWrap = styled.div`
  display: flex;
`;

const extraCss = css`
  margin-left: 1rem;
  margin-bottom: 0.5rem;
`;

const CertificateItemDetail = (props) => {
  const {
    ListDetailHeaderBg,
    renderContent,
    backToLists,
    owner,
    container,
  } = props;

  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);

  const goBackToList = () => {
    backToLists();
  };

  return (
    <ComponentError>
      <Section>
        {isMobileScreen ? (
          <BackButton onClick={goBackToList}>
            <BackArrow />
            <TitleOne extraCss="font-weight:bold;margin-left:1rem;">
              {owner !== 'N/A' ? 'Review Details' : 'No Certificates Added'}
            </TitleOne>
          </BackButton>
        ) : null}
        <ColumnHeader>
          <HeaderBg bgImage={ListDetailHeaderBg} />
          <div className="list-title-wrap">
            {!isMobileScreen && (
              <ListTitle>
                {owner !== 'N/A' ? 'Review Details' : 'No Certificates Added'}
              </ListTitle>
            )}
            <DetailWrap>
              <TitleThree color="#8b8ea6">Container:</TitleThree>
              <TitleThree color="#c4c4c4" extraCss={extraCss}>
                {container}
              </TitleThree>
            </DetailWrap>
            <DetailWrap>
              <TitleThree color="#8b8ea6">Owner Email:</TitleThree>
              <TitleThree color="#c4c4c4" extraCss={extraCss}>
                {owner}
              </TitleThree>
            </DetailWrap>
          </div>
        </ColumnHeader>
        {renderContent}
      </Section>
    </ComponentError>
  );
};
CertificateItemDetail.propTypes = {
  ListDetailHeaderBg: PropTypes.string,
  renderContent: PropTypes.node,
  backToLists: PropTypes.func,
  owner: PropTypes.string,
  container: PropTypes.string,
};

CertificateItemDetail.defaultProps = {
  ListDetailHeaderBg: '',
  renderContent: <div />,
  backToLists: () => {},
  owner: 'N/A',
  container: 'N/A',
};

export default CertificateItemDetail;
