/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
/* eslint-disable react/no-unused-prop-types */
/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import sectionHeaderBg from 'assets/Banner_img.png';
import { TitleFour } from 'styles/GlobalStyles';
import SelectionTabs from '../Tabs';

// styled components goes here
const Section = styled('section')``;

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  background-size: contain;
  background-repeat: no-repeat;
  padding: 2.5rem 2rem;
  background-image: url(${(props) => props.headerBgSrc || ''});
  .safe-title-wrap {
    width: 70%;
  }
`;

const SafeTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography};
  margin: 1rem 0 1.2rem;
  text-overflow: ellipsis;
  overflow: hidden;
`;
const SafeDetails = (props) => {
  const { detailData, params } = props;
  const safeDetail =
    (detailData &&
      detailData.filter(
        (safe) => safe.safeName === params.match?.params.safeName
      )) ||
    {};
  return (
    <ComponentError>
      {' '}
      <Section>
        <ColumnHeader headerBgSrc={sectionHeaderBg}>
          <div className="safe-title-wrap">
            <SafeTitle>{safeDetail?.safeName || 'No Safe'}</SafeTitle>
            <TitleFour color="#c4c4c4">
              {safeDetail?.description ||
                'Create a Safe to see your secrets, folders and permissions here'}
            </TitleFour>
          </div>
        </ColumnHeader>
        <SelectionTabs secrets={safeDetail.secrets} />
      </Section>
    </ComponentError>
  );
};
SafeDetails.propTypes = {
  detailData: PropTypes.array,
  params: PropTypes.object,
};
SafeDetails.defaultProps = {
  detailData: [],
  params: {},
};

export default SafeDetails;
