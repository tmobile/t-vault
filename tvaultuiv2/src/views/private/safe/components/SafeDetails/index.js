/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
/* eslint-disable react/no-unused-prop-types */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useHistory, useLocation } from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import sectionHeaderBg from '../../../../../assets/Banner_img.png';
import { BackArrow } from '../../../../../assets/SvgIcons';
import { TitleFour } from '../../../../../styles/GlobalStyles';
import mediaBreakpoints from '../../../../../breakpoints';
import SelectionTabs from '../Tabs';

// styled components goes here
const Section = styled('section')`
  background-image: url(${(props) => props.headerBgSrc || ''});
  background-size: contain;
  background-repeat: no-repeat;
  position: absolute;
  z-index: 2;
  width: 100%;
  height: 100%;
  top: -8px;
`;

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-right: 2rem;
  height: 16rem;
  .safe-title-wrap {
    width: 70%;
  }
  ${mediaBreakpoints.small} {
    height: 13rem;
    padding: 1rem;
  }
`;

const SafeTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography};
  margin: 1rem 0 1.2rem;
  text-overflow: ellipsis;
  overflow: hidden;
`;
const BackButton = styled.div`
  display: flex;
  align-items: center;
  padding: 2rem 0 0 2rem;
  span {
    margin-left: 1rem;
  }
`;

const SafeDetails = (props) => {
  const { setActiveSafeFolders, detailData } = props;
  const [safeDetail, setSafeDetail] = useState({});
  // use history of page
  const history = useHistory();
  const location = useLocation();
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  // route component data
  const goBackToSafeList = () => {
    setActiveSafeFolders();
    history.goBack();
  };
  useEffect(() => {
    if (!location?.state?.safe) {
      if (detailData && detailData.length) {
        const activeSafeDetail = detailData.filter(
          (item) =>
            item?.name?.toLowerCase() ===
            history.location.pathname.split('/')[2]
        );
        setSafeDetail(activeSafeDetail);
      }
      return;
    }
    setSafeDetail(location?.state?.safe);
  }, [location.state, detailData, history.location.pathname]);

  return (
    <ComponentError>
      <Section headerBgSrc={sectionHeaderBg}>
        {isMobileScreen ? (
          <BackButton onClick={goBackToSafeList}>
            <BackArrow />
            <span>{safeDetail.name || 'No safe'}</span>
          </BackButton>
        ) : null}
        <ColumnHeader>
          <div className="safe-title-wrap">
            {!isMobileScreen && (
              <SafeTitle>{safeDetail?.name || 'No Safe'}</SafeTitle>
            )}
            <TitleFour color="#c4c4c4">
              {safeDetail?.description
                ? safeDetail?.description
                : 'Create a Safe to see your secrets, folders and permissions here'}
            </TitleFour>
          </div>
        </ColumnHeader>
        <SelectionTabs safeDetail={safeDetail} />
      </Section>
    </ComponentError>
  );
};
SafeDetails.propTypes = {
  detailData: PropTypes.array,
  params: PropTypes.object,
  setActiveSafeFolders: PropTypes.func,
};
SafeDetails.defaultProps = {
  detailData: [],
  params: {},
  setActiveSafeFolders: () => {},
};

export default SafeDetails;
