import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

const TabPanelWrapper = styled.div`
  height: 90%;
`;
const TabPanel = (props) => {
  const { children, value, index } = props;
  return (
    <TabPanelWrapper
      role="tabpanel"
      hidden={value !== index}
      id={`scrollable-prevent-tabpanel-${index}`}
      aria-labelledby={`scrollable-prevent-tab-${index}`}
    >
      {children}
    </TabPanelWrapper>
  );
};

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
};

export default TabPanel;
