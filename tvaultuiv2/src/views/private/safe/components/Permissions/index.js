/* eslint-disable no-console */
/* eslint-disable react/jsx-props-no-spreading */
import React, { useState } from 'react';
import styled, { css } from 'styled-components';
import { makeStyles } from '@material-ui/core/styles';
import Tab from '@material-ui/core/Tab';
import PropTypes from 'prop-types';
import AppBar from '@material-ui/core/AppBar';
import Tabs from '@material-ui/core/Tabs';
import ComponentError from '../../../../../errorBoundaries/ComponentError/component-error';
import NamedButton from '../../../../../components/NamedButton';
import permissionPlusIcon from '../../../../../assets/permission-plus.svg';
import mediaBreakpoints from '../../../../../breakpoints';
import User from './components/User';

const { small } = mediaBreakpoints;

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

const CountPlusWrapper = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
`;
const CountSpan = styled.div`
  color: #5e627c;
  font-size: 1.3rem;
`;

const customStyles = css`
  display: flex;
  ${small} {
    display: none;
  }
`;

const customMobileStyles = css`
  display: none;
  ${small} {
    display: flex;
  }
`;

const useStyles = makeStyles(() => ({
  appBar: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
}));

const Permissions = (props) => {
  const { safeDetail } = props;
  const classes = useStyles();
  const [value, setValue] = useState(0);
  const [newPermission, setNewPermission] = useState(false);
  const [count, setCount] = useState(0);

  const handleChange = (event, newValue) => {
    setValue(newValue);
  };

  const getPermissionCount = (val) => {
    setCount(val);
  };

  return (
    <ComponentError>
      <>
        <CountPlusWrapper>
          <CountSpan color="#5e627c">{`${count} Permissions`}</CountSpan>
          <NamedButton
            customStyle={customMobileStyles}
            label="Add Permission"
            iconSrc={permissionPlusIcon}
            onClick={() => setNewPermission(true)}
          />
        </CountPlusWrapper>
        <TabWrapper>
          <AppBar position="static" className={classes.appBar}>
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
            <NamedButton
              customStyle={customStyles}
              label="Add Permission"
              iconSrc={permissionPlusIcon}
              onClick={() => setNewPermission(true)}
            />
          </AppBar>
          <TabPanel value={value} index={0}>
            <User
              safeDetail={safeDetail}
              newPermission={newPermission}
              onNewPermissionChange={() => setNewPermission(false)}
              getPermissionCount={(val) => getPermissionCount(val)}
            />
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
      </>
    </ComponentError>
  );
};

Permissions.propTypes = {
  safeDetail: PropTypes.objectOf(PropTypes.any),
};

Permissions.defaultProps = {
  safeDetail: {},
};

export default Permissions;
