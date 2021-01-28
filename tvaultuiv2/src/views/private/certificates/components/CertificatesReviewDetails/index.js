import React, { useState, useEffect } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import Loader from '../../../../../components/Loaders/LoaderSpinner';
import NoData from '../../../../../components/NoData';
import Strings from '../../../../../resources';
import noCertificatesIcon from '../../../../../assets/no-certficates.svg';
import CertificateSelectionTabs from '../Tab';

const DetailsContainer = styled.section`
  background-image: linear-gradient(to bottom, #151820, #2c3040);
  height: calc(100% - 17.1rem);
`;
const customStyle = css`
  height: 100%;
`;
const NoDataWrapper = styled.div`
  display: flex;
  justify-content: center;
  height: 100%;
`;
const bgIconStyle = {
  width: '20rem',
  height: '20rem',
};

const noDataStyle = css`
  height: 100%;
  width: 50%;
  justify-content: center;
`;

const CertificatesReviewDetails = (props) => {
  const { certificateDetail } = props;
  const [response, setResponse] = useState({ status: 'loading' });

  useEffect(() => {
    if (certificateDetail) {
      setResponse({ status: 'success' });
    }
  }, [certificateDetail]);
  return (
    <ComponentError>
      <DetailsContainer>
        {response.status === 'loading' && <Loader customStyle={customStyle} />}
        {response.status === 'success' && (
          <>
            {Object.keys(certificateDetail).length === 0 && (
              <NoDataWrapper>
                <NoData
                  imageSrc={noCertificatesIcon}
                  description={Strings.Resources.noCertificatesFound}
                  bgIconStyle={bgIconStyle}
                  customStyle={noDataStyle}
                />
              </NoDataWrapper>
            )}
            {Object.keys(certificateDetail).length > 0 && (
              <CertificateSelectionTabs certificateDetail={certificateDetail} />
            )}
          </>
        )}
      </DetailsContainer>
    </ComponentError>
  );
};

CertificatesReviewDetails.propTypes = {
  certificateDetail: PropTypes.objectOf(PropTypes.any),
};

CertificatesReviewDetails.defaultProps = {
  certificateDetail: {},
};

export default CertificatesReviewDetails;
