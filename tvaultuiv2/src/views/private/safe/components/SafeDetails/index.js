/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
/* eslint-disable react/no-unused-prop-types */
/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import sectionHeaderBg from 'assets/safe-header-bg.png';
import SelectionTabs from '../Tabs';

// styled components goes here
const Section = styled('section')``;

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  background-size: contain;
  background-repeat: no-repeat;
  background-image: url(${(props) => props.headerBgSrc || ''});
  .safe-title-wrap {
    width: 70%;
  }
`;

const SafeDescription = styled.p`
  font-size: 1.4em;
  text-align: left;
`;
const SafeTitle = styled('h5')`
  font-size: ${(props) => props.theme.typography};
`;
const SafeDetails = (props) => {
  const { detailData, params } = props;
  const compData =
    (detailData && detailData[params.match?.params.safeName]) || {};

  return (
    <ComponentError>
      {' '}
      <Section>
        <ColumnHeader headerBgSrc={sectionHeaderBg}>
          <div className="safe-title-wrap">
            <SafeTitle>No safes yet</SafeTitle>
            <SafeDescription>{compData.description}</SafeDescription>
          </div>
        </ColumnHeader>
        <SelectionTabs secrets={compData.secrets} />
      </Section>
    </ComponentError>
  );
};
SafeDetails.propTypes = {
  detailData: PropTypes.object,
  params: PropTypes.object,
};
SafeDetails.defaultProps = {
  detailData: {},
  params: {},
};

export default SafeDetails;
