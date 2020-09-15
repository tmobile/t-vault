/* eslint-disable react/jsx-props-no-spreading */
/* eslint-disable import/no-unresolved */
import React, { useState } from 'react';
import styled from 'styled-components';
import ComponentError from 'errorBoundaries/ComponentError/component-error';
import Tab from '@material-ui/core/Tab';
import PropTypes from 'prop-types';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import User from './components/User';

const TabPanelWrapper = styled.div``;

function TabPanel(props) {
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
}

TabPanel.propTypes = {
  children: PropTypes.node,
  index: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
};

TabPanel.defaultProps = {
  children: <div />,
};

function a11yProps(index) {
  return {
    id: `scrollable-prevent-tab-${index}`,
    'aria-controls': `scrollable-prevent-tabpanel-${index}`,
  };
}

const TabWrapper = styled.div`
  .MuiAppBar-colorPrimary {
    background-color: inherit;
  }
  .MuiPaper-elevation4 {
    box-shadow: none;
  }
  .MuiTabs-root {
    min-height: unset;
  }
  .MuiTab-textColorInherit.Mui-selected {
    background-color: ${(props) => props.theme.palette.secondary.main};
    color: ${(props) => props.theme.palette.secondary.contrastText};
  }
  .MuiTab-textColorInherit {
    color: #e8e8e8;
    background-color: #20232e;
    min-width: auto;
    padding: 8.5px 20px;
    margin-right: 0.5rem;
    font-size: 1.4rem;
    min-height: 3.65rem;
  }
  .PrivateTabIndicator-colorSecondary-15 {
    background-color: inherit;
  }
`;

const CountSpan = styled.div`
  margin-bottom: 1.85rem;
  color: #5e627c;
  font-size: 1.3rem;
`;

const Permissions = () => {
  const [value, setValue] = useState(0);
  const [users] = useState([]);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };
  return (
    <ComponentError>
      <CountSpan color="#5e627c">
        {`${users && users.length} Permissions`}
      </CountSpan>
      <TabWrapper>
        <AppBar position="static">
          <Tabs
            value={value}
            onChange={handleChange}
            variant="scrollable"
            scrollButtons="off"
            aria-label="scrollable prevent tabs example"
          >
            <Tab label="User" {...a11yProps(0)} />
            <Tab label="Group" {...a11yProps(1)} />
            <Tab label="AWS Application" {...a11yProps(2)} />
            <Tab label="App Roles" {...a11yProps(3)} />
          </Tabs>
        </AppBar>
        <TabPanel value={value} index={0}>
          <User users={users} />
        </TabPanel>
        <TabPanel value={value} index={1}>
          Group
        </TabPanel>
        <TabPanel value={value} index={2}>
          Aws
        </TabPanel>
        <TabPanel value={value} index={3}>
          App Roles
        </TabPanel>
      </TabWrapper>
    </ComponentError>
  );
};

export default Permissions;
