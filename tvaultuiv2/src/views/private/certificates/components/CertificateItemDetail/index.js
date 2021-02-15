import React from 'react';
import PropTypes from 'prop-types';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import { BackArrow } from '../../../../../assets/SvgIcons';
import mediaBreakpoints from '../../../../../breakpoints';
import { TitleThree } from '../../../../../styles/GlobalStyles';
import Strings from '../../../../../resources';

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
  background-color: #151820;
  .list-title-wrap {
    width: 67%;
    z-index: 2;
    ${mediaBreakpoints.small} {
      // width: 60%;
      margin-top: 2rem;
    }
  }
  ${mediaBreakpoints.small} {
    height: 18rem;
    padding: 1rem;
  }
`;

const BackButton = styled.div`
  display: flex;
  align-items: center;
  position: absolute;
  z-index: 2;
  top: 1.5rem;
  left: 1rem;
  span {
    width: 28rem;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    font-size: 1.8rem;
    font-weight: bold;
  }
`;

const HeaderBg = styled('div')`
  position: absolute;
  top: -0.8rem;
  left: 0;
  right: 0;
  bottom: 0;
  background: url(${(props) => props.bgImage || ''});
  background-size: cover;
  background-repeat: no-repeat;
  ${mediaBreakpoints.small} {
    z-index: -1;
    top: 0;
  }
`;

const ListTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography.h5};
  margin: 1rem 0 1.2rem;
  text-overflow: ellipsis;
  overflow: hidden;
  word-break: break-all;
  width: 100%;
  text-overflow: ellipsis;
  overflow: hidden;
  white-space: nowrap;
`;

const CertificateItemDetail = (props) => {
  const { ListDetailHeaderBg, renderContent, backToLists, name } = props;

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
            <span>{name !== 'N/A' ? name : '...'}</span>
          </BackButton>
        ) : null}
        <ColumnHeader>
          <HeaderBg bgImage={ListDetailHeaderBg} />
          <div className="list-title-wrap">
            {!isMobileScreen && (
              <ListTitle>{name !== 'N/A' ? name : '...'}</ListTitle>
            )}
            <TitleThree color="#c4c4c4">
              {Strings.Resources.certificateDesc}
            </TitleThree>
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
  name: PropTypes.string,
};

CertificateItemDetail.defaultProps = {
  ListDetailHeaderBg: '',
  renderContent: <div />,
  backToLists: () => {},
  name: 'N/A',
};

export default CertificateItemDetail;
