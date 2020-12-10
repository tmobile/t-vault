/* eslint-disable no-nested-ternary */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import { useHistory, useLocation } from 'react-router-dom';
import useMediaQuery from '@material-ui/core/useMediaQuery';
import styled from 'styled-components';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import sectionHeaderBg from '../../../../../assets/Banner_img.png';
import sectionMobHeaderBg from '../../../../../assets/mob-safebg.svg';
import sectionTabHeaderBg from '../../../../../assets/tab-safebg.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import ListDetailHeader from '../../../../../components/ListDetailHeader';

// styled components goes here
const Section = styled('section')`
  flex-direction: column;
  display: flex;
  z-index: 2;
  width: 100%;
  height: 100%;
`;

const SafeDetails = (props) => {
  const { detailData, resetClicked, renderContent } = props;
  const [safe, setSafe] = useState({});
  // use history of page
  const history = useHistory();
  const location = useLocation();
  // screen view handler
  const isMobileScreen = useMediaQuery(mediaBreakpoints.small);
  const isTabScreen = useMediaQuery(mediaBreakpoints.medium);
  // route component data
  const goBackToSafeList = () => {
    resetClicked();
  };

  useEffect(() => {
    if (detailData && detailData.length) {
      const activeSafeDetail = detailData.filter(
        (item) =>
          item?.name?.toLowerCase() === history.location.pathname.split('/')[2]
      );
      setSafe(activeSafeDetail[0]);
    }
  }, [location, detailData, history.location.pathname]);

  return (
    <ComponentError>
      <Section>
        <ListDetailHeader
          title={safe?.name || 'No Safe'}
          description={
            safe?.description ||
            'A Safe is a logical unit to store the secrets. All the safes are created within Vault. You can control access only at the safe level. As a vault administrator you can manage safes but cannot view the content of the safe.'
          }
          bgImage={
            isMobileScreen
              ? sectionMobHeaderBg
              : isTabScreen
              ? sectionTabHeaderBg
              : sectionHeaderBg
          }
          goBackToList={goBackToSafeList}
        />
        {renderContent}
      </Section>
    </ComponentError>
  );
};
SafeDetails.propTypes = {
  detailData: PropTypes.arrayOf(PropTypes.any),
  resetClicked: PropTypes.func,
  renderContent: PropTypes.node,
};
SafeDetails.defaultProps = {
  detailData: [],
  resetClicked: () => {},
  renderContent: <div />,
};

export default SafeDetails;
