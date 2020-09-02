/* eslint-disable react/forbid-prop-types */
/* eslint-disable react/require-default-props */
/* eslint-disable react/no-unused-prop-types */
/* eslint-disable import/no-unresolved */
import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import SelectionTabs from 'components/containers/Tabs';

// styled components goes here
const Section = styled('section')`
  &:last-child {
    border-right: none;
  }
`;

const ColumnHeader = styled('div')`
  display: flex;
  align-items: center;
  padding: 0.5em;
  justify-content: space-between;
`;
const SafeIconWrap = styled('div')`
  width: 20%;
  height: 100%;
  background: #4a4a4a;
  border-radious: 50%;
  align-items: center;
  display: flex;
`;
const SafeDescription = styled.p`
  font-size: 1.4em;
  text-align: left;
`;
const SafeDetails = (props) => {
  const { detailData, params } = props;
  const compdata = detailData[params.match.params.safename];
  return (
    <Section>
      <ColumnHeader>
        <SafeIconWrap />
        <SafeDescription>{compdata.description}</SafeDescription>
      </ColumnHeader>
      <SelectionTabs secrets={compdata.secrets} />
    </Section>
  );
};
SafeDetails.propTypes = {
  detailData: PropTypes.object,
  params: PropTypes.object,
};

export default SafeDetails;
